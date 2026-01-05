package com.easyride.matching_service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easyride.matching_service.config.MatchingConfig;
import com.easyride.matching_service.dto.*;
import com.easyride.matching_service.model.DriverStatus;
import com.easyride.matching_service.model.GrabbableOrder;
import com.easyride.matching_service.model.GrabbableOrderStatus;
import com.easyride.matching_service.model.MatchingRecord;
import com.easyride.matching_service.repository.DriverStatusMapper;
import com.easyride.matching_service.repository.GrabbableOrderMapper;
import com.easyride.matching_service.repository.MatchingRecordMapper;
import com.easyride.matching_service.rocket.MatchingEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MatchingServiceImpl implements MatchingService {

    private final DriverStatusMapper driverStatusMapper;
    private final MatchingRecordMapper matchingRecordMapper;
    private final MatchingEventProducer matchingEventProducer;
    private final GrabbableOrderMapper grabbableOrderMapper;
    private final MatchingConfig matchingConfig;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String DRIVER_LOCATIONS_KEY = "driver:locations";

    public MatchingServiceImpl(DriverStatusMapper driverStatusMapper,
            MatchingRecordMapper matchingRecordMapper,
            MatchingEventProducer matchingEventProducer,
            MatchingConfig matchingConfig,
            GrabbableOrderMapper grabbableOrderMapper,
            RedisTemplate<String, String> redisTemplate) {
        this.driverStatusMapper = driverStatusMapper;
        this.matchingRecordMapper = matchingRecordMapper;
        this.matchingEventProducer = matchingEventProducer;
        this.matchingConfig = matchingConfig;
        this.grabbableOrderMapper = grabbableOrderMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public DriverAssignedEventDto findAndAssignDriver(MatchRequestDto matchRequest) {
        log.info("Attempting to find driver for order: {}", matchRequest.getOrderId());

        // Use Redis Geo to find nearby drivers first for better performance
        double radius = matchingConfig.getMaxMatchRadiusKm();
        Circle circle = new Circle(new Point(matchRequest.getStartLongitude(), matchRequest.getStartLatitude()),
                new Distance(radius, Metrics.KILOMETERS));

        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance()
                .sortAscending();

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo()
                .radius(DRIVER_LOCATIONS_KEY, circle, args);

        if (results == null || results.getContent().isEmpty()) {
            log.warn("No drivers found within {}km for order {}", radius, matchRequest.getOrderId());
            matchingEventProducer.sendOrderMatchFailedEvent(
                    new OrderMatchFailedEventDto(matchRequest.getOrderId(), "NO_DRIVERS_NEARBY"));
            return null;
        }

        List<RankedDriver> rankedDrivers = results.getContent().stream()
                .map(result -> {
                    String driverIdStr = result.getContent().getName();
                    Long driverId = Long.parseLong(driverIdStr);
                    double distanceToPickup = result.getDistance().getValue();

                    DriverStatus driver = driverStatusMapper.selectById(driverId);
                    if (driver == null)
                        return null;

                    // Preliminary checks
                    if (!driver.isAvailable()) {
                        // Remove from Redis if not actually available
                        redisTemplate.opsForZSet().remove(DRIVER_LOCATIONS_KEY, driverIdStr);
                        return null;
                    }

                    if (matchRequest.getVehicleTypeRequired() != null &&
                            !matchRequest.getVehicleTypeRequired().equalsIgnoreCase(driver.getVehicleType())) {
                        return null;
                    }

                    if (driver.getOnlineSince() != null &&
                            Duration.between(driver.getOnlineSince(), LocalDateTime.now()).toHours() > matchingConfig
                                    .getMaxDriverWorkHours()) {
                        log.debug("Driver {} has exceeded max work hours.", driver.getDriverId());
                        return null;
                    }

                    if ("CARPOOL".equalsIgnoreCase(matchRequest.getServiceType())) {
                        if (driver.getCurrentPassengersInCar() >= driver.getMaxCapacityForCarpool()) {
                            log.debug("Driver {} is full for carpool.", driver.getDriverId());
                            return null;
                        }
                    }

                    // Ranking logic
                    double distanceScore = (matchingConfig.getMaxMatchRadiusKm() - distanceToPickup)
                            / matchingConfig.getMaxMatchRadiusKm();
                    double ratingScore = driver.getRating() / 5.0;

                    double preferenceBoost = 0.0;
                    if (matchRequest.getPreferredDriverId() != null
                            && matchRequest.getPreferredDriverId().equals(driver.getDriverId())) {
                        preferenceBoost += 0.2;
                        log.debug("Applying preference boost for driver {}", driver.getDriverId());
                    }

                    double overallScore = (ratingScore * matchingConfig.getRatingWeight()) +
                            (distanceScore * matchingConfig.getDistanceWeight()) +
                            preferenceBoost;

                    return new RankedDriver(driver, overallScore, distanceToPickup);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(RankedDriver::getScore).reversed())
                .collect(Collectors.toList());

        if (rankedDrivers.isEmpty()) {
            log.warn("No suitable drivers found after filtering and ranking for order {}", matchRequest.getOrderId());
            matchingEventProducer.sendOrderMatchFailedEvent(
                    new OrderMatchFailedEventDto(matchRequest.getOrderId(), "NO_SUITABLE_DRIVERS_FOUND"));
            return null;
        }

        DriverStatus bestDriver = rankedDrivers.get(0).getDriver();
        double etaToPickupMinutes = (rankedDrivers.get(0).getDistanceToPickupKm() / 25.0) * 60.0;

        bestDriver.setAvailable(false);
        if ("CARPOOL".equalsIgnoreCase(matchRequest.getServiceType())) {
            bestDriver.setCurrentPassengersInCar(bestDriver.getCurrentPassengersInCar() + 1);
        }
        bestDriver.setLastStatusUpdateTime(LocalDateTime.now());
        if (driverStatusMapper.updateById(bestDriver) == 0) {
            log.warn("Failed to assign driver {} due to concurrent update.", bestDriver.getDriverId());
            // In a real scenario, you might retry or try the next ranked driver.
            return null;
        }

        MatchingRecord record = MatchingRecord.builder()
                .orderId(matchRequest.getOrderId())
                .driverId(bestDriver.getDriverId())
                .matchedTime(LocalDateTime.now())
                .status("ASSIGNED")
                .matchStrategy("AUTOMATIC")
                .success(true)
                .build();
        matchingRecordMapper.insert(record);

        log.info("Successfully matched driver {} to order {}", bestDriver.getDriverId(), matchRequest.getOrderId());

        DriverAssignedEventDto assignedEvent = new DriverAssignedEventDto(
                matchRequest.getOrderId(),
                bestDriver.getDriverId(),
                "Driver " + bestDriver.getDriverId(),
                "PLATE XYZ",
                bestDriver.getVehicleType() + " Vehicle",
                bestDriver.getRating(),
                LocalDateTime.now().plusMinutes((long) etaToPickupMinutes));

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

        public DriverStatus getDriver() {
            return driver;
        }

        public double getScore() {
            return score;
        }

        public double getDistanceToPickupKm() {
            return distanceToPickupKm;
        }
    }

    @Override
    @Transactional
    public void updateDriverStatus(Long driverId, DriverStatusUpdateDto statusUpdateDto) {
        log.info("Updating status for driver ID {}: {}", driverId, statusUpdateDto);
        DriverStatus driverStatus = driverStatusMapper.selectById(driverId);
        if (driverStatus == null) {
            log.warn("DriverStatus not found for ID {} during update attempt.", driverId);
            throw new RuntimeException("司机状态记录未找到: " + driverId);
        }

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
        driverStatusMapper.updateById(driverStatus);

        // Update Redis Geo information
        if (driverStatus.isAvailable()) {
            redisTemplate.opsForGeo().add(DRIVER_LOCATIONS_KEY,
                    new Point(driverStatus.getCurrentLongitude(), driverStatus.getCurrentLatitude()),
                    driverId.toString());
        } else {
            redisTemplate.opsForZSet().remove(DRIVER_LOCATIONS_KEY, driverId.toString());
        }

        log.info("DriverStatus for ID {} updated successfully in DB and Redis.", driverId);
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
                LocalDateTime.now().plusMinutes(10));
        grabbableOrderMapper.insert(grabbableOrder);
    }

    @Override
    public List<AvailableOrderDto> getAvailableOrdersForDriver() {
        List<GrabbableOrder> pendingOrders = grabbableOrderMapper.selectList(new LambdaQueryWrapper<GrabbableOrder>()
                .eq(GrabbableOrder::getStatus, GrabbableOrderStatus.PENDING_GRAB)
                .gt(GrabbableOrder::getExpiryTime, LocalDateTime.now()));
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
                        null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean acceptOrder(Long orderId, Long driverId) {
        log.info("Driver {} attempting to accept/grab order {}", driverId, orderId);
        GrabbableOrder grabbableOrder = grabbableOrderMapper.selectById(orderId);
        DriverStatus driver = driverStatusMapper.selectById(driverId);

        if (driver == null || !driver.isAvailable()) {
            log.warn("Driver {} not found or not available to accept order {}", driverId, orderId);
            return false;
        }

        if (grabbableOrder != null) {
            if (grabbableOrder.getStatus() == GrabbableOrderStatus.PENDING_GRAB &&
                    grabbableOrder.getExpiryTime().isAfter(LocalDateTime.now())) {

                grabbableOrder.setStatus(GrabbableOrderStatus.GRABBED);
                grabbableOrder.setGrabbingDriverId(driverId);
                grabbableOrderMapper.updateById(grabbableOrder);

                driver.setAvailable(false);
                driver.setLastStatusUpdateTime(LocalDateTime.now());
                driverStatusMapper.updateById(driver);

                // For simplicity, directly assign here.
                grabbableOrder.setStatus(GrabbableOrderStatus.ASSIGNED);
                grabbableOrderMapper.updateById(grabbableOrder);

                MatchingRecord record = MatchingRecord.builder()
                        .orderId(orderId)
                        .driverId(driverId)
                        .matchedTime(LocalDateTime.now())
                        .matchStrategy("ASSIGNED_MANUAL")
                        .status("ASSIGNED")
                        .success(true)
                        .build();
                matchingRecordMapper.insert(record);

                DriverAssignedEventDto assignedEvent = new DriverAssignedEventDto(
                        orderId, driverId, "Driver " + driverId,
                        "PLATE GRAB", driver.getVehicleType(), driver.getRating(),
                        LocalDateTime.now().plusMinutes(10));
                matchingEventProducer.sendDriverAssignedEvent(assignedEvent);
                log.info("Order {} successfully grabbed and assigned to driver {}", orderId, driverId);
                return true;
            } else {
                log.warn("Order {} is no longer available for grabbing (status: {}, expired: {})",
                        orderId, grabbableOrder.getStatus(),
                        grabbableOrder.getExpiryTime().isBefore(LocalDateTime.now()));
            }
        } else {
            log.warn("Grabbable order {} not found.", orderId);
        }
        return false;
    }
}