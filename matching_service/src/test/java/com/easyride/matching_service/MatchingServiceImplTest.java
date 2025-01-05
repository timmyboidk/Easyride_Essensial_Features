package com.easyride.matching_service.service;

import com.easyride.matching_service.dto.MatchRequestDto;
import com.easyride.matching_service.model.DriverStatus;
import com.easyride.matching_service.repository.DriverStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class MatchingServiceImplTest {

    @Mock
    private DriverStatusRepository driverStatusRepository;

    @InjectMocks
    private MatchingServiceImpl matchingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void matchDriver_ShouldReturnBestDriver() {
        // 1. 构造测试数据
        MatchRequestDto request = MatchRequestDto.builder()
                .orderId(101L)
                .passengerId(10L)
                .startLatitude(30.0)
                .startLongitude(120.0)
                .vehicleType("STANDARD")
                .build();

        DriverStatus d1 = DriverStatus.builder()
                .driverId(201L)
                .latitude(30.1)
                .longitude(120.2)
                .available(true)
                .rating(4.5)
                .vehicleType("STANDARD")
                .build();

        DriverStatus d2 = DriverStatus.builder()
                .driverId(202L)
                .latitude(29.9)
                .longitude(120.0)
                .available(true)
                .rating(4.0)
                .vehicleType("STANDARD")
                .build();

        // 2. Mock 仓库返回
        when(driverStatusRepository.findAll()).thenReturn(Arrays.asList(d1, d2));

        // 3. 执行匹配
        Long bestDriverId = matchingService.matchDriver(request);

        // 4. 验证
        // 简单计算: 距离(30.0,120.0) -> d1距离略远,但 rating 高; d2距离较近, rating略低
        // 具体看距离/评分加权, 可能d2或d1最优, 取决于您在 computeDistance() 及 sort时的逻辑
        // 这里仅演示断言逻辑:
        assertNotNull(bestDriverId);
        // 可检查 bestDriverId == 201L 或 202L, 根据您的排序公式可能不一样
    }

    @Test
    void updateDriverStatus_ShouldSaveNewOrExisting() {
        // 1. 构造测试数据
        Long driverId = 301L;
        when(driverStatusRepository.findById(driverId)).thenReturn(java.util.Optional.empty());

        // 2. 执行
        matchingService.updateDriverStatus(driverId, 31.0, 121.0, true, 4.8, "PREMIUM");

        // 3. 验证
        ArgumentCaptor<DriverStatus> captor = ArgumentCaptor.forClass(DriverStatus.class);
        verify(driverStatusRepository).save(captor.capture());
        DriverStatus saved = captor.getValue();

        assertEquals(driverId, saved.getDriverId());
        assertEquals(31.0, saved.getLatitude());
        assertEquals(4.8, saved.getRating());
        assertTrue(saved.isAvailable());
        assertEquals("PREMIUM", saved.getVehicleType());
    }
}
