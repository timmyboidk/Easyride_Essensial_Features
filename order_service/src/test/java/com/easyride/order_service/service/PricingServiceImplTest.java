package com.easyride.order_service.service;

import com.easyride.order_service.dto.EstimatedPriceInfo;
import com.easyride.order_service.dto.FinalPriceInfo;
import com.easyride.order_service.dto.LocationDto;
import com.easyride.order_service.dto.PricingContext;
import com.easyride.order_service.exception.PricingException;
import com.easyride.order_service.model.Order;
import com.easyride.order_service.model.OrderStatus;
import com.easyride.order_service.model.ServiceType;
import com.easyride.order_service.model.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class PricingServiceImplTest {

    private PricingServiceImpl pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingServiceImpl();
    }

    @Test
    void calculateEstimatedPrice_Normal_Success() {
        PricingContext context = PricingContext.builder()
                .startLocation(new LocationDto(31.2304, 121.4737))
                .endLocation(new LocationDto(31.2222, 121.4581))
                .serviceType(ServiceType.EXPRESS)
                .build();

        EstimatedPriceInfo info = pricingService.calculateEstimatedPrice(context);

        assertNotNull(info);
        assertTrue(info.getEstimatedCost() > 0);
        assertTrue(info.getEstimatedDistance() > 0);
    }

    @Test
    void calculateEstimatedPrice_Carpool_Success() {
        PricingContext context = PricingContext.builder()
                .startLocation(new LocationDto(31.2304, 121.4737))
                .endLocation(new LocationDto(31.2222, 121.4581))
                .serviceType(ServiceType.CARPOOL)
                .build();

        EstimatedPriceInfo info = pricingService.calculateEstimatedPrice(context);

        assertNotNull(info);
    }

    @Test
    void calculateEstimatedPrice_MissingLocation_ThrowsException() {
        PricingContext context = PricingContext.builder()
                .startLocation(null)
                .endLocation(new LocationDto(31.2222, 121.4581))
                .build();

        assertThrows(PricingException.class, () -> pricingService.calculateEstimatedPrice(context));
    }

    @Test
    void calculateEstimatedPrice_MorningPeak_Surcharge() {
        // Peak hour: 7-9 AM
        LocalDateTime peakTime = LocalDateTime.now().withHour(8).withMinute(0);
        PricingContext context = PricingContext.builder()
                .startLocation(new LocationDto(31.2304, 121.4737))
                .endLocation(new LocationDto(31.2222, 121.4581))
                .serviceType(ServiceType.EXPRESS)
                .scheduledTime(peakTime)
                .build();

        EstimatedPriceInfo info = pricingService.calculateEstimatedPrice(context);
        assertTrue(info.getPriceBreakdown().contains("Peak Surge"));
    }

    @Test
    void calculateEstimatedPrice_EveningPeak_Surcharge() {
        // Peak hour: 5-7 PM (17-19)
        LocalDateTime peakTime = LocalDateTime.now().withHour(18).withMinute(0);
        PricingContext context = PricingContext.builder()
                .startLocation(new LocationDto(31.2304, 121.4737))
                .endLocation(new LocationDto(31.2222, 121.4581))
                .serviceType(ServiceType.EXPRESS)
                .scheduledTime(peakTime)
                .build();

        EstimatedPriceInfo info = pricingService.calculateEstimatedPrice(context);
        assertTrue(info.getPriceBreakdown().contains("Peak Surge"));
    }

    @Test
    void calculateEstimatedPrice_AirportTransfer_Success() {
        PricingContext context = PricingContext.builder()
                .startLocation(new LocationDto(31.2304, 121.4737))
                .endLocation(new LocationDto(31.2222, 121.4581))
                .serviceType(ServiceType.AIRPORT_TRANSFER)
                .build();

        EstimatedPriceInfo info = pricingService.calculateEstimatedPrice(context);
        assertNotNull(info);
    }

    @Test
    void calculateEstimatedPrice_CharterHourly_Success() {
        PricingContext context = PricingContext.builder()
                .startLocation(new LocationDto(31.2304, 121.4737))
                .serviceType(ServiceType.CHARTER_HOURLY)
                .scheduledTime(LocalDateTime.now())
                .actualDurationMinutes(120.0) // 2 hours
                .build();

        EstimatedPriceInfo info = pricingService.calculateEstimatedPrice(context);
        assertEquals(100.0, info.getEstimatedCost()); // 50 * 2
    }

    @Test
    void calculateFinalPrice_Success() {
        Order order = new Order();
        order.setDriverAssignedTime(LocalDateTime.now().minusMinutes(20));
        order.setOrderTime(LocalDateTime.now());
        order.setStartLatitude(31.2304);
        order.setStartLongitude(121.4737);
        order.setEndLatitude(31.2222);
        order.setEndLongitude(121.4581);

        PricingContext context = PricingContext.builder()
                .startLocation(new LocationDto(31.2304, 121.4737))
                .endLocation(new LocationDto(31.2222, 121.4581))
                .build();

        FinalPriceInfo info = pricingService.calculateFinalPrice(order, context);

        assertNotNull(info);
        assertEquals(20, info.getActualDuration());
    }

    @Test
    void calculateFinalPrice_MissingTimes_ThrowsException() {
        Order order = new Order(); // missing times
        PricingContext context = PricingContext.builder().build();
        assertThrows(PricingException.class, () -> pricingService.calculateFinalPrice(order, context));
    }

    @Test
    void calculateCancellationFee_Scheduled_Free() {
        Order order = new Order();
        order.setStatus(OrderStatus.SCHEDULED);
        order.setScheduledTime(LocalDateTime.now().plusHours(2));

        double fee = pricingService.calculateCancellationFee(order, LocalDateTime.now());

        assertEquals(0.0, fee);
    }

    @Test
    void calculateCancellationFee_Scheduled_WithFee() {
        Order order = new Order();
        order.setStatus(OrderStatus.SCHEDULED);
        order.setScheduledTime(LocalDateTime.now().plusMinutes(30));

        double fee = pricingService.calculateCancellationFee(order, LocalDateTime.now());

        assertTrue(fee > 0);
    }

    @Test
    void calculateCancellationFee_Arrived_WithFee() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.ARRIVED);
        order.setUpdatedAt(LocalDateTime.now().minusMinutes(10));

        double fee = pricingService.calculateCancellationFee(order, LocalDateTime.now());

        assertEquals(8.0, fee); // CANCELLATION_FEE_ARRIVED
    }

    @Test
    void calculateCancellationFee_InFreeWindow() {
        Order order = new Order();
        order.setStatus(OrderStatus.DRIVER_ASSIGNED);
        order.setUpdatedAt(LocalDateTime.now().minusMinutes(2)); // Within 5 min window

        double fee = pricingService.calculateCancellationFee(order, LocalDateTime.now());
        assertEquals(0.0, fee);
    }
}
