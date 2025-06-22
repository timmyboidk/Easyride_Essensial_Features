package com.easyride.matching_service.service;

import com.easyride.matching_service.config.MatchingConfig;
import com.easyride.matching_service.dto.*;
import com.easyride.matching_service.model.DriverStatus;
import com.easyride.matching_service.model.GrabbableOrder;
import com.easyride.matching_service.model.GrabbableOrderStatus;
import com.easyride.matching_service.model.MatchingRecord;
import com.easyride.matching_service.repository.DriverStatusRepository;
import com.easyride.matching_service.repository.GrabbableOrderRepository;
import com.easyride.matching_service.repository.MatchingRecordRepository;
import com.easyride.matching_service.rocket.MatchingEventProducer;
import com.easyride.matching_service.util.HaversineDistanceUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MatchingServiceImpl implements MatchingService {

    private final DriverStatusRepository driverStatusRepository;
    private final MatchingRecordRepository matchingRecordRepository;
    private final MatchingEventProducer matchingEventProducer;
    private final GrabbableOrderRepository grabbableOrderRepository;
    private final MatchingConfig matchingConfig;

    @Autowired
    public MatchingServiceImpl(DriverStatusRepository driverStatusRepository,
                               MatchingRecordRepository matchingRecordRepository,
                               MatchingEventProducer matchingEventProducer,
                               MatchingConfig matchingConfig,
                               GrabbableOrderRepository grabbableOrderRepository) {
        this.driverStatusRepository = driverStatusRepository;
        this.matchingRecordRepository = matchingRecordRepository;
        this.matchingEventProducer = matchingEventProducer;
        this.matchingConfig = matchingConfig;
        this.grabbableOrderRepository = grabbableOrderRepository;
    }

    @Override
    @Transactional
    public DriverAssignedEventDto findAndAssignDriver(MatchRequestDto matchRequest) {
        log.info("Attempting to find driver for order: {}", matchRequest.getOrderId());

        List<DriverStatus> candidateDrivers = driverStatusRepository.findByAvailableTrueAndVehicleType(
                matchRequest.getVehicleTypeRequired()
        );
        log.info("Found {} initial candidate drivers for vehicle type {}", candidateDrivers.size(), matchRequest.getVehicleTypeRequired());

        if (candidateDrivers.isEmpty()) {
            log.warn("No available drivers found for order {}", matchRequest.getOrderId());
            matchingEventProducer.sendOrderMatchFailedEvent(
                    new OrderMatchFailedEventDto(matchRequest.getOrderId(), "NO_DRIVERS_AVAILABLE")
            );
            return null;
        }

        List<RankedDriver> rankedDrivers = candidateDrivers.stream()
                .map(driver -> {
                    double distanceToPickup = HaversineDistanceUtil.calculateDistance(
                            driver.getCurrentLatitude(), driver.getCurrentLongitude(),
                            matchRequest.getStartLatitude(), matchRequest.getStartLongitude()
                    );

                    if (distanceToPickup > matchingConfig.getMaxMatchRadiusKm()) {
                        log.debug("Driver {} (Dist: {}km) is outside max radius of {}km for order {}",
                                driver.getDriverId(), distanceToPickup, matchingConfig.getMaxMatchRadiusKm(), matchRequest.getOrderId());
                        return null;
                    }

                    if (driver.getOnlineSince() != null &&
                            Duration.between(driver.getOnlineSince(), LocalDateTime.now()).toHours() > matchingConfig.getMaxDriverWorkHours()) {
                        log.debug("Driver {} has exceeded max work hours.", driver.getDriverId());
                        return null;
                    }

                    if ("CARPOOL".equalsIgnoreCase(matchRequest.getServiceType())) {
                        if (driver.getCurrentPassengersInCar() >= driver.getMaxCapacityForCarpool()) {
                            log.debug("Driver {} is full for carpool.", driver.getDriverId());
                            return null;
                        }
                    }

                    double distanceScore = (matchingConfig.getMaxMatchRadiusKm() - distanceToPickup) / matchingConfig.getMaxMatchRadiusKm();
                    double ratingScore = driver.getRating() / 5.0;

                    double preferenceBoost = 0.0;
                    if (matchRequest.getPreferredDriverId() != null && matchRequest.getPreferredDriverId().equals(driver.getDriverId())) {
                        preferenceBoost += 0.2;
                        log.debug("Applying preference boost for driver {}", driver.getDriverId());
                    }

                    double overallScore = (ratingScore * matchingConfig.getRatingWeight()) +
                            (distanceScore * matchingConfig.getDistanceWeight()) +
                            preferenceBoost;

                    log.debug("Driver {}: Dist={}km (Score={}), Rating={} (Score={}), PrefBoost={}, Overall={}",
                            driver.getDriverId(), distanceToPickup, distanceScore, driver.getRating(), ratingScore, preferenceBoost, overallScore);

                    return new RankedDriver(driver, overallScore, distanceToPickup);
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingDouble(RankedDriver::getScore).reversed())
                .collect(Collectors.toList());

        if (rankedDrivers.isEmpty()) {
            log.warn("No suitable drivers found after filtering and ranking for order {}", matchRequest.getOrderId());
            matchingEventProducer.sendOrderMatchFailedEvent(
                    new OrderMatchFailedEventDto(matchRequest.getOrderId(), "NO_SUITABLE_DRIVERS_FOUND")
            );
            return null;
        }

        DriverStatus bestDriver = rankedDrivers.get(0).getDriver();
        double etaToPickupMinutes = (rankedDrivers.get(0).getDistanceToPickupKm() / 25.0) * 60.0;

        bestDriver.setAvailable(false);
        if ("CARPOOL".equalsIgnoreCase(matchRequest.getServiceType())) {
            bestDriver.setCurrentPassengersInCar(bestDriver.getCurrentPassengersInCar() + 1);
        }
        bestDriver.setLastStatusUpdateTime(LocalDateTime.now());
        driverStatusRepository.save(bestDriver);

        MatchingRecord record = MatchingRecord.builder()
                .orderId(matchRequest.getOrderId())
                .driverId(bestDriver.getDriverId())
                .matchedTime(LocalDateTime.now())
                .status("ASSIGNED")
                .matchStrategy("AUTOMATIC")
                .success(true)
                .build();
        matchingRecordRepository.save(record);

        log.info("Successfully matched driver {} to order {}", bestDriver.getDriverId(), matchRequest.getOrderId());

        DriverAssignedEventDto assignedEvent = new DriverAssignedEventDto(
                matchRequest.getOrderId(),
                bestDriver.getDriverId(),
                "Driver " + bestDriver.getDriverId(),
                "PLATE XYZ",
                bestDriver.getVehicleType() + " Vehicle",
                bestDriver.getRating(),
                LocalDateTime.now().plusMinutes((long) etaToPickupMinutes)
        );

        matchingEventProducer.sendDriverAssignedEvent(assignedEvent);

        return assignedEvent;
    }

    private static class RankedDriver {
        private final DriverStatus driver;
        private final double score;
        private final double distanceToPickupKm;

        public RankedDriver(DriverStatus driver, double score, double distanceToPickupKm) {
            this.driver = driver;
            this.score = score;
            this.distanceToPickupKm = distanceToPickupKm;
        }
        public DriverStatus getDriver() { return driver; }
        public double getScore() { return score; }
        public double getDistanceToPickupKm() { return distanceToPickupKm; }
    }

    @Override
    @Transactional
    public void updateDriverStatus(Long driverId, DriverStatusUpdateDto statusUpdateDto) {
        log.info("Updating status for driver ID {}: {}", driverId, statusUpdateDto);
        DriverStatus driverStatus = driverStatusRepository.findById(driverId)
                .orElseThrow(() -> {
                    log.warn("DriverStatus not found for ID {} during update attempt.", driverId);
                    return new RuntimeException("司机状态记录未找到: " + driverId);
                });

        driverStatus.setAvailable(statusUpdateDto.getAvailable());
        if (statusUpdateDto.getCurrentLatitude() != null && statusUpdateDto.getCurrentLongitude() != null) {
            driverStatus.setCurrentLatitude(statusUpdateDto.getCurrentLatitude());
            driverStatus.setCurrentLongitude(statusUpdateDto.getCurrentLongitude());
            driverStatus.setLastLocationUpdateTime(LocalDateTime.now());
        }
        if (statusUpdateDto.getCurrentCity() != null) {
            driverStatus.setCurrentCity(statusUpdateDto.getCurrentCity());
        }

        if (statusUpdateDto.getAvailable() && driverStatus.getOnlineSince() == null) {
            driverStatus.setOnlineSince(LocalDateTime.now());
        } else if (!statusUpdateDto.getAvailable()) {
            driverStatus.setOnlineSince(null);
        }

        driverStatus.setLastStatusUpdateTime(LocalDateTime.now());
        driverStatusRepository.save(driverStatus);
        log.info("DriverStatus for ID {} updated successfully.", driverId);
    }

    @Override
    public void makeOrderAvailableForGrabbing(MatchRequestDto matchRequest) {
        log.info("Making order {} available for grabbing.", matchRequest.getOrderId());
        GrabbableOrder grabbableOrder = new GrabbableOrder(
                matchRequest.getOrderId(),
                matchRequest.getStartLatitude(),
                matchRequest.getStartLongitude(),
                matchRequest.getStartAddressFormatted(),
                matchRequest.getEndLatitude(),
                matchRequest.getEndLongitude(),
                matchRequest.getEndAddressFormatted(),
                matchRequest.getVehicleTypeRequired(),
                matchRequest.getServiceType(),
                matchRequest.getEstimatedCost(),
                matchRequest.getOrderTime(),
                LocalDateTime.now().plusMinutes(10)
        );
        grabbableOrderRepository.save(grabbableOrder);
    }

    @Override
    public List<AvailableOrderDto> getAvailableOrdersForDriver() {
        List<GrabbableOrder> pendingOrders = grabbableOrderRepository.findByStatusAndExpiryTimeAfter(
                GrabbableOrderStatus.PENDING_GRAB, LocalDateTime.now()
        );
        log.info("Found {} pending grabbable orders.", pendingOrders.size());

        return pendingOrders.stream()
                .map(order -> new AvailableOrderDto(
                        order.getOrderId(),
                        order.getStartAddressFormatted(),
                        order.getEndAddressFormatted(),
                        order.getServiceType(),
                        order.getVehicleTypeRequired(),
                        order.getEstimatedFare(),
                        order.getOrderTime(),
                        null
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean acceptOrder(Long orderId, Long driverId) {
        log.info("Driver {} attempting to accept/grab order {}", driverId, orderId);
        Optional<GrabbableOrder> grabbableOrderOpt = grabbableOrderRepository.findById(orderId);
        Optional<DriverStatus> driverOpt = driverStatusRepository.findById(driverId);

        if (driverOpt.isEmpty() || !driverOpt.get().isAvailable()) {
            log.warn("Driver {} not found or not available to accept order {}", driverId, orderId);
            return false;
        }

        DriverStatus driver = driverOpt.get();

        if (grabbableOrderOpt.isPresent()) {
            GrabbableOrder grabbableOrder = grabbableOrderOpt.get();
            if (grabbableOrder.getStatus() == GrabbableOrderStatus.PENDING_GRAB &&
                    grabbableOrder.getExpiryTime().isAfter(LocalDateTime.now())) {

                grabbableOrder.setStatus(GrabbableOrderStatus.GRABBED);
                grabbableOrder.setGrabbingDriverId(driverId);
                grabbableOrderRepository.save(grabbableOrder);

                driver.setAvailable(false);
                driver.setLastStatusUpdateTime(LocalDateTime.now());
                driverStatusRepository.save(driver);

                // For simplicity, directly assign here.
                grabbableOrder.setStatus(GrabbableOrderStatus.ASSIGNED);
                grabbableOrderRepository.save(grabbableOrder);

                MatchingRecord record = MatchingRecord.builder()
                        .orderId(orderId)
                        .driverId(driverId)
                        .matchedTime(LocalDateTime.now())
                        .matchStrategy("ASSIGNED_MANUAL")
                        .status("ASSIGNED")
                        .success(true)
                        .build();
                matchingRecordRepository.save(record);

                DriverAssignedEventDto assignedEvent = new DriverAssignedEventDto(
                        orderId, driverId, "Driver " + driverId,
                        "PLATE GRAB", driver.getVehicleType(), driver.getRating(),
                        LocalDateTime.now().plusMinutes(10)
                );
                matchingEventProducer.sendDriverAssignedEvent(assignedEvent);
                log.info("Order {} successfully grabbed and assigned to driver {}", orderId, driverId);
                return true;
            } else {
                log.warn("Order {} is no longer available for grabbing (status: {}, expired: {})",
                        orderId, grabbableOrder.getStatus(), grabbableOrder.getExpiryTime().isBefore(LocalDateTime.now()));
            }
        } else {
            log.warn("Grabbable order {} not found.", orderId);
        }
        return false;
    }
}