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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.domain.geo.Metrics;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceImplTest {

    @Mock
    private DriverStatusRepository driverStatusRepository;
    @Mock
    private MatchingRecordRepository matchingRecordRepository;
    @Mock
    private MatchingEventProducer matchingEventProducer;
    @Mock
    private GrabbableOrderRepository grabbableOrderRepository;
    @Mock
    private MatchingConfig matchingConfig;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private GeoOperations<String, String> geoOperations;
    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private MatchingServiceImpl matchingService;

    @BeforeEach
    void setUp() {
        // Manually instantiate to avoid injection issues
        matchingService = new MatchingServiceImpl(
                driverStatusRepository,
                matchingRecordRepository,
                matchingEventProducer,
                matchingConfig,
                grabbableOrderRepository,
                redisTemplate);

        lenient().when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        lenient().when(matchingConfig.getMaxMatchRadiusKm()).thenReturn(10.0);
        lenient().when(matchingConfig.getRatingWeight()).thenReturn(0.5);
        lenient().when(matchingConfig.getDistanceWeight()).thenReturn(0.5);
        lenient().when(matchingConfig.getMaxDriverWorkHours()).thenReturn(8);
    }

    @Test
    void findAndAssignDriver_Success() {
        MatchRequestDto request = new MatchRequestDto();
        request.setOrderId(100L);
        request.setStartLatitude(30.0);
        request.setStartLongitude(120.0);
        request.setVehicleTypeRequired("ECONOMY");

        // Mock Redis results
        RedisGeoCommands.GeoLocation<String> location = new RedisGeoCommands.GeoLocation<>("201",
                new Point(120.01, 30.01));
        GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult = new GeoResult<>(location,
                new Distance(1.0, Metrics.KILOMETERS));
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = new GeoResults<>(
                Collections.singletonList(geoResult));

        when(geoOperations.radius(anyString(), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(geoResults);

        // Mock Database result
        DriverStatus driver = new DriverStatus();
        driver.setDriverId(201L);
        driver.setAvailable(true);
        driver.setVehicleType("ECONOMY");
        driver.setRating(4.8);
        when(driverStatusRepository.findById(201L)).thenReturn(Optional.of(driver));

        DriverAssignedEventDto result = matchingService.findAndAssignDriver(request);

        assertNotNull(result);
        assertEquals(201L, result.getDriverId());
        verify(driverStatusRepository).save(any(DriverStatus.class));
        verify(matchingRecordRepository).save(any(MatchingRecord.class));
        verify(matchingEventProducer).sendDriverAssignedEvent(any(DriverAssignedEventDto.class));
    }

    @Test
    void findAndAssignDriver_NoDriversNearby() {
        MatchRequestDto request = new MatchRequestDto();
        request.setOrderId(101L);
        request.setStartLatitude(30.0);
        request.setStartLongitude(120.0);

        when(geoOperations.radius(anyString(), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(new GeoResults<>(Collections.emptyList()));

        DriverAssignedEventDto result = matchingService.findAndAssignDriver(request);

        assertNull(result);
        verify(matchingEventProducer).sendOrderMatchFailedEvent(any(OrderMatchFailedEventDto.class));
    }

    @Test
    void updateDriverStatus_Available() {
        Long driverId = 301L;
        DriverStatusUpdateDto dto = new DriverStatusUpdateDto();
        dto.setAvailable(true);
        dto.setCurrentLatitude(31.0);
        dto.setCurrentLongitude(121.0);

        DriverStatus status = new DriverStatus();
        status.setDriverId(driverId);
        when(driverStatusRepository.findById(driverId)).thenReturn(Optional.of(status));

        matchingService.updateDriverStatus(driverId, dto);

        verify(driverStatusRepository).save(status);
        verify(geoOperations).add(eq("driver:locations"), any(Point.class), eq("301"));
        assertTrue(status.isAvailable());
    }

    @Test
    void updateDriverStatus_Unavailable() {
        Long driverId = 301L;
        DriverStatusUpdateDto dto = new DriverStatusUpdateDto();
        dto.setAvailable(false);

        DriverStatus status = new DriverStatus();
        status.setDriverId(driverId);
        when(driverStatusRepository.findById(driverId)).thenReturn(Optional.of(status));

        matchingService.updateDriverStatus(driverId, dto);

        verify(driverStatusRepository).save(status);
        verify(zSetOperations).remove("driver:locations", "301");
        assertFalse(status.isAvailable());
    }

    @Test
    void acceptOrder_Success() {
        Long orderId = 500L;
        Long driverId = 201L;

        GrabbableOrder order = new GrabbableOrder();
        order.setOrderId(orderId);
        order.setStatus(GrabbableOrderStatus.PENDING_GRAB);
        order.setExpiryTime(LocalDateTime.now().plusHours(1));

        DriverStatus driver = new DriverStatus();
        driver.setDriverId(driverId);
        driver.setAvailable(true);

        when(grabbableOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(driverStatusRepository.findById(driverId)).thenReturn(Optional.of(driver));

        boolean accepted = matchingService.acceptOrder(orderId, driverId);

        assertTrue(accepted);
        assertEquals(GrabbableOrderStatus.ASSIGNED, order.getStatus());
        assertEquals(driverId, order.getGrabbingDriverId());
        assertFalse(driver.isAvailable());
        verify(matchingRecordRepository).save(any(MatchingRecord.class));
        verify(matchingEventProducer).sendDriverAssignedEvent(any(DriverAssignedEventDto.class));
    }

    @Test
    void acceptOrder_DriverUnavailable() {
        Long orderId = 500L;
        Long driverId = 201L;

        DriverStatus driver = new DriverStatus();
        driver.setDriverId(driverId);
        driver.setAvailable(false);

        when(driverStatusRepository.findById(driverId)).thenReturn(Optional.of(driver));

        boolean accepted = matchingService.acceptOrder(orderId, driverId);

        assertFalse(accepted);
    }
}
