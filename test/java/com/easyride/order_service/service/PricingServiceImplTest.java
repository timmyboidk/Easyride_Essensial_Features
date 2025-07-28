package com.easyride.order_service.service;

import com.easyride.order_service.dto.EstimatedPriceInfo;
import com.easyride.order_service.dto.LocationDto;
import com.easyride.order_service.dto.PricingContext;
import com.easyride.order_service.exception.PricingException;
import com.easyride.order_service.model.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceImplTest {

    // 直接实例化被测试的类，因为没有任何外部依赖
    private final PricingService pricingService = new PricingServiceImpl();

    private LocationDto startLocation;
    private LocationDto endLocation;

    @BeforeEach
    void setUp() {
        // 为了简化，我们假设起点和终点固定，距离约为10公里
        // 并且平均速度为30km/h, 估算行程时间为20分钟
        startLocation = new LocationDto(40.7128, -74.0060); // New York City
        endLocation = new LocationDto(40.7580, -73.9855);   // Times Square (approx. 10km away by road)
    }

    // --- 测试场景1: 标准服务类型在非高峰时段 ---
    @Test
    void calculateEstimatedPrice_forNormalService_nonPeakHour() {
        // 1. 准备 (Arrange)
        PricingContext context = PricingContext.builder()
                .startLocation(startLocation)
                .endLocation(endLocation)
                .serviceType(ServiceType.NORMAL)
                // 设置一个非高峰时段的时间 (e.g., 14:00)
                .scheduledTime(LocalDateTime.now().with(LocalTime.of(14, 0)))
                .build();

        // 2. 执行 (Act)
        EstimatedPriceInfo priceInfo = pricingService.calculateEstimatedPrice(context);

        // 3. 断言 (Assert)
        // 基础费用(2.5) + 里程费(1.5 * 10km = 15) + 时间费(0.2 * 20min = 4) = 21.5
        // 我们使用 assertEquals 的 delta 参数来处理浮点数计算的精度问题
        assertEquals(21.5, priceInfo.getEstimatedCost(), 0.01);
    }

    // --- 测试场景2: 标准服务类型在高峰时段 ---
    @Test
    void calculateEstimatedPrice_forNormalService_peakHour() {
        // 1. 准备 (Arrange)
        PricingContext context = PricingContext.builder()
                .startLocation(startLocation)
                .endLocation(endLocation)
                .serviceType(ServiceType.NORMAL)
                // 设置一个高峰时段的时间 (e.g., 8:00)
                .scheduledTime(LocalDateTime.now().with(LocalTime.of(8, 0)))
                .build();

        // 2. 执行 (Act)
        EstimatedPriceInfo priceInfo = pricingService.calculateEstimatedPrice(context);

        // 3. 断言 (Assert)
        // 基础费用(2.5) + 里程费(15) + 时间费(4) = 21.5
        // 高峰附加费(21.5 * 0.2) = 4.3
        // 总费用 = 21.5 + 4.3 = 25.8
        assertEquals(25.8, priceInfo.getEstimatedCost(), 0.01);
    }

    // --- 测试场景3: 机场接送服务 ---
    @Test
    void calculateEstimatedPrice_forAirportTransfer_shouldAddSurcharge() {
        // 1. 准备 (Arrange)
        PricingContext context = PricingContext.builder()
                .startLocation(startLocation)
                .endLocation(endLocation)
                .serviceType(ServiceType.AIRPORT_TRANSFER)
                .scheduledTime(LocalDateTime.now().with(LocalTime.of(14, 0)))
                .build();

        // 2. 执行 (Act)
        EstimatedPriceInfo priceInfo = pricingService.calculateEstimatedPrice(context);

        // 3. 断言 (Assert)
        // 标准费用(21.5) + 机场附加费(5.0) = 26.5
        assertEquals(26.5, priceInfo.getEstimatedCost(), 0.01);
    }

    // --- 测试场景4: 输入不合法 ---
    @Test
    void calculateEstimatedPrice_whenLocationMissing_shouldThrowException() {
        // 1. 准备 (Arrange)
        PricingContext context = PricingContext.builder()
                .startLocation(startLocation)
                // 故意不设置终点
                .endLocation(null)
                .serviceType(ServiceType.NORMAL)
                .build();

        // 2. 执行 & 断言
        PricingException exception = assertThrows(PricingException.class, () -> {
            pricingService.calculateEstimatedPrice(context);
        });
        assertEquals("Start or end location missing for price estimation.", exception.getMessage());
    }
}