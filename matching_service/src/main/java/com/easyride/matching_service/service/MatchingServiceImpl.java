package com.easyride.matching_service.service;

import com.easyride.matching_service.config.MatchingConfig;
import com.easyride.matching_service.dto.*;
import com.easyride.matching_service.model.DriverStatus;
import com.easyride.matching_service.model.MatchingRecord;
import com.easyride.matching_service.repository.DriverStatusRepository;
import com.easyride.matching_service.repository.MatchingRecordRepository;
import com.easyride.matching_service.rocket.MatchingEventProducer;
import com.easyride.matching_service.util.HaversineDistanceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
// import org.apache.rocketmq.spring.core.RocketMQTemplate; // Keep if still needed for direct location req
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MatchingServiceImpl implements MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingServiceImpl.class);

    private final DriverStatusRepository driverStatusRepository;
    private final MatchingRecordRepository matchingRecordRepository;
    private final MatchingEventProducer matchingEventProducer;
    private final MatchingConfig matchingConfig;
    // private final RocketMQTemplate rocketMQTemplate; // For location requests

    @Autowired
    public MatchingServiceImpl(DriverStatusRepository driverStatusRepository,
                               MatchingRecordRepository matchingRecordRepository,
                               MatchingEventProducer matchingEventProducer,
                               MatchingConfig matchingConfig
            /* RocketMQTemplate rocketMQTemplate */) {
        this.driverStatusRepository = driverStatusRepository;
        this.matchingRecordRepository = matchingRecordRepository;
        this.matchingEventProducer = matchingEventProducer;
        this.matchingConfig = matchingConfig;
        // this.rocketMQTemplate = rocketMQTemplate;
    }

    @Override
    @Transactional
    public DriverAssignedEventDto findAndAssignDriver(MatchRequestDto matchRequest) {
        log.info("Attempting to find driver for order: {}", matchRequest.getOrderId());

        // 1. Fetch available drivers matching basic criteria
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

        // 2. Filter and Score drivers
        List<RankedDriver> rankedDrivers = candidateDrivers.stream()
                .map(driver -> {
                    double distanceToPickup = HaversineDistanceUtil.calculateDistance(
                            driver.getCurrentLatitude(), driver.getCurrentLongitude(),
                            matchRequest.getStartLatitude(), matchRequest.getStartLongitude()
                    );

                    // Filter by max radius
                    if (distanceToPickup > matchingConfig.getMaxMatchRadiusKm()) {
                        log.debug("Driver {} (Dist: {}km) is outside max radius of {}km for order {}",
                                driver.getDriverId(), distanceToPickup, matchingConfig.getMaxMatchRadiusKm(), matchRequest.getOrderId());
                        return null;
                    }

                    // Filter by work hours (simplified example)
                    if (driver.getOnlineSince() != null &&
                            Duration.between(driver.getOnlineSince(), LocalDateTime.now()).toHours() > matchingConfig.getMaxDriverWorkHours()) {
                        log.debug("Driver {} has exceeded max work hours.", driver.getDriverId());
                        return null;
                    }

                    // Carpool specific filtering
                    if ("CARPOOL".equalsIgnoreCase(matchRequest.getServiceType())) {
                        if (driver.getCurrentPassengersInCar() >= driver.getMaxCapacityForCarpool()) {
                            log.debug("Driver {} is full for carpool.", driver.getDriverId());
                            return null; // Driver's car is full
                        }
                    }


                    // Scoring: Higher score is better
                    // Distance score: invert distance (closer is better), normalize if needed
                    double distanceScore = (matchingConfig.getMaxMatchRadiusKm() - distanceToPickup) / matchingConfig.getMaxMatchRadiusKm(); // Normalized 0-1

                    // Rating score: directly use rating (assuming 0-5)
                    double ratingScore = driver.getRating() / 5.0; // Normalized 0-1

                    // Passenger preferences (example: preferred driver ID)
                    double preferenceBoost = 0.0;
                    if (matchRequest.getPreferredDriverId() != null && matchRequest.getPreferredDriverId().equals(driver.getDriverId())) {
                        preferenceBoost += 0.2; // Add a significant boost
                        log.debug("Applying preference boost for driver {}", driver.getDriverId());
                    }
                    // TODO: Add scoring for preferredDriverTags (if driver has matching tags)
                    // TODO: Add scoring for driver's preferred service areas (if matchRequest.startAddressFormatted can be matched to area)


                    double overallScore = (ratingScore * matchingConfig.getRatingWeight()) +
                            (distanceScore * matchingConfig.getDistanceWeight()) +
                            preferenceBoost;

                    log.debug("Driver {}: Dist={}km (Score={}), Rating={} (Score={}), PrefBoost={}, Overall={}",
                            driver.getDriverId(), distanceToPickup, distanceScore, driver.getRating(), ratingScore, preferenceBoost, overallScore);

                    return new RankedDriver(driver, overallScore, distanceToPickup);
                })
                .filter(java.util.Objects::nonNull) // Remove nulls from filtering
                .sorted(Comparator.comparingDouble(RankedDriver::getScore).reversed()) // Best score first
                .collect(Collectors.toList());

        if (rankedDrivers.isEmpty()) {
            log.warn("No suitable drivers found after filtering and ranking for order {}", matchRequest.getOrderId());
            matchingEventProducer.sendOrderMatchFailedEvent(
                    new OrderMatchFailedEventDto(matchRequest.getOrderId(), "NO_SUITABLE_DRIVERS_FOUND")
            );
            return null;
        }

        // 3. Select best driver and attempt assignment (can involve trying top N drivers)
        DriverStatus bestDriver = rankedDrivers.get(0).getDriver();
        double etaToPickupMinutes = (rankedDrivers.get(0).getDistanceToPickupKm() / 25.0) * 60.0; // Assuming avg speed 25km/h for ETA


        // Simulate "offering" the ride to the driver or directly assigning
        // In a more complex system, you might send an "ORDER_OFFERED" event to the driver's app
        // and wait for their acceptance. For now, direct assignment.

        bestDriver.setAvailable(false); // Mark driver as busy
        if ("CARPOOL".equalsIgnoreCase(matchRequest.getServiceType())) {
            bestDriver.setCurrentPassengersInCar(bestDriver.getCurrentPassengersInCar() + 1); // Assuming 1 passenger per carpool order segment
        }
        bestDriver.setLastStatusUpdateTime(LocalDateTime.now());
        driverStatusRepository.save(bestDriver);

        MatchingRecord record = new MatchingRecord();
        record.setOrderId(matchRequest.getOrderId());
        record.setDriverId(bestDriver.getDriverId());
        record.setMatchTime(LocalDateTime.now());
        record.setStatus("ASSIGNED");
        matchingRecordRepository.save(record);

        log.info("Successfully matched driver {} to order {}", bestDriver.getDriverId(), matchRequest.getOrderId());

        DriverAssignedEventDto assignedEvent = new DriverAssignedEventDto(
                matchRequest.getOrderId(),
                bestDriver.getDriverId(),
                "Driver " + bestDriver.getDriverId(), // Placeholder name
                "PLATE XYZ", // Placeholder, fetch from DriverStatus or User Service
                bestDriver.getVehicleType() + " Vehicle", // Placeholder
                bestDriver.getRating(),
                LocalDateTime.now().plusMinutes((long) etaToPickupMinutes)
        );

        // Prompt 3: Reliably publish DRIVER_ASSIGNED event
        matchingEventProducer.sendDriverAssignedEvent(assignedEvent);

        return assignedEvent;
    }

    // Helper class for ranking
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

    // ... (inside MatchingServiceImpl class)
    @Override
    @Transactional
    public void updateDriverStatus(Long driverId, DriverStatusUpdateDto statusUpdateDto) {
        log.info("Updating status for driver ID {}: {}", driverId, statusUpdateDto);
        DriverStatus driverStatus = driverStatusRepository.findById(driverId)
                .orElseThrow(() -> {
                    log.warn("DriverStatus not found for ID {} during update attempt.", driverId);
                    // Optionally, create if not exists, but usually status updates are for existing drivers.
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
            driverStatus.setOnlineSince(null); // Reset when offline
        }

        driverStatus.setLastStatusUpdateTime(LocalDateTime.now());
        driverStatusRepository.save(driverStatus);
        log.info("DriverStatus for ID {} updated successfully.", driverId);
    }

    @Autowired
    private GrabbableOrderRepository grabbableOrderRepository;

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
                LocalDateTime.now().plusMinutes(10) // Example: available for 10 mins
        );
        grabbableOrderRepository.save(grabbableOrder);
        // Optionally, publish an event that new grabbable orders are available,
        // or drivers poll the /orders/available endpoint.
    }

    @Override
    public List<AvailableOrderDto> getAvailableOrdersForDriver(/* Long driverId, DriverPreferences preferences */) {
        // Fetch current driver's status to get their location
        // DriverStatus currentDriverStatus = driverStatusRepository.findById(driverId).orElse(null);
        // if (currentDriverStatus == null) return Collections.emptyList();

        List<GrabbableOrder> pendingOrders = grabbableOrderRepository.findByStatusAndExpiryTimeAfter(
                GrabbableOrderStatus.PENDING_GRAB, LocalDateTime.now()
        );
        log.info("Found {} pending grabbable orders.", pendingOrders.size());

        return pendingOrders.stream()
                // .filter(order -> matchesDriverPreferences(order, preferences, currentDriverStatus)) // Further filter
                .map(order -> new AvailableOrderDto(
                        order.getOrderId(),
                        order.getStartAddressFormatted(),
                        order.getEndAddressFormatted(),
                        order.getServiceType(),
                        order.getVehicleTypeRequired(),
                        order.getEstimatedFare(),
                        order.getOrderTime(),
                        // Calculate distance from this driver to pickup if driver location is known
                        // currentDriverStatus != null ? HaversineDistanceUtil.calculateDistance(currentDriverStatus.getCurrentLatitude(), currentDriverStatus.getCurrentLongitude(), order.getStartLatitude(), order.getStartLongitude()) : null
                        null // Placeholder for distanceToPickupKm
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean acceptOrder(Long orderId, Long driverId) {
        log.info("Driver {} attempting to accept/grab order {}", driverId, orderId);
        Optional<GrabbableOrder> grabbableOrderOpt = grabbableOrderRepository.findById(orderId);
        DriverStatus driver = driverStatusRepository.findById(driverId).orElse(null);

        if (driver == null || !driver.isAvailable()) {
            log.warn("Driver {} not found or not available to accept order {}", driverId, orderId);
            return false;
        }

        if (grabbableOrderOpt.isPresent()) {
            GrabbableOrder grabbableOrder = grabbableOrderOpt.get();
            if (grabbableOrder.getStatus() == GrabbableOrderStatus.PENDING_GRAB &&
                    grabbableOrder.getExpiryTime().isAfter(LocalDateTime.now())) {

                grabbableOrder.setStatus(GrabbableOrderStatus.GRABBED); // Tentatively grabbed
                grabbableOrder.setGrabbingDriverId(driverId);
                grabbableOrderRepository.save(grabbableOrder);

                // Mark driver busy
                driver.setAvailable(false);
                driver.setLastStatusUpdateTime(LocalDateTime.now());
                driverStatusRepository.save(driver);

                // Finalize assignment and notify Order Service
                // TODO: Consider a short confirmation window or direct assignment.
                // For simplicity, directly assign here.
                grabbableOrder.setStatus(GrabbableOrderStatus.ASSIGNED);
                grabbableOrderRepository.save(grabbableOrder);

                MatchingRecord record = new MatchingRecord(orderId, driverId, LocalDateTime.now(), "ASSIGNED_MANUAL");
                matchingRecordRepository.save(record);

                DriverAssignedEventDto assignedEvent = new DriverAssignedEventDto(
                        orderId, driverId, "Driver " + driverId,
                        "PLATE GRAB", driver.getVehicleType(), driver.getRating(),
                        LocalDateTime.now().plusMinutes(10) // Placeholder ETA
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
            // It might be an order that needs auto-matching
        }
        return false;
    }
}
