package com.easyride.order_service.service;

import com.easyride.order_service.dto.*;
import com.easyride.order_service.exception.OrderServiceException;
import com.easyride.order_service.model.*;
import com.easyride.order_service.repository.*;
import com.easyride.order_service.rocket.OrderEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private PassengerMapper passengerMapper;
    @Mock
    private DriverMapper driverMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PricingService pricingService;
    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderCreateDto orderCreateDto;
    private Passenger passenger;
    private EstimatedPriceInfo estimatedPriceInfo;

    @BeforeEach
    void setUp() {
        orderCreateDto = new OrderCreateDto();
        orderCreateDto.setPassengerId(1L);
        orderCreateDto.setStartLocation(new LocationDto(31.2304, 121.4737));
        orderCreateDto.setEndLocation(new LocationDto(31.2222, 121.4581));
        orderCreateDto.setVehicleType(VehicleType.ECONOMY);
        orderCreateDto.setServiceType(ServiceType.EXPRESS);
        orderCreateDto.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        passenger = new Passenger();
        passenger.setId(1L);
        passenger.setUsername("testuser");

        estimatedPriceInfo = EstimatedPriceInfo.builder()
                .estimatedCost(25.0)
                .estimatedDistance(5.0)
                .estimatedDuration(15.0)
                .build();
    }

    @Test
    void createOrder_Success_Immediate() {
        when(passengerMapper.selectById(1L)).thenReturn(passenger);
        when(pricingService.calculateEstimatedPrice(any())).thenReturn(estimatedPriceInfo);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(100L);
            return 1;
        });

        OrderResponseDto response = orderService.createOrder(orderCreateDto);

        assertNotNull(response);
        assertEquals(100L, response.getOrderId());
        assertEquals(OrderStatus.PENDING_MATCH, response.getStatus());
        verify(orderEventProducer, times(1)).sendOrderCreatedEvent(any());
        verify(orderMapper, times(1)).insert(any(Order.class));
    }

    @Test
    void createOrder_PassengerNotFound_CreatesPlaceholder() {
        when(passengerMapper.selectById(1L)).thenReturn(null);
        when(passengerMapper.insert(any(Passenger.class))).thenReturn(1);
        when(pricingService.calculateEstimatedPrice(any())).thenReturn(estimatedPriceInfo);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(100L);
            return 1;
        });

        orderService.createOrder(orderCreateDto);

        verify(passengerMapper, times(1)).insert(any(Passenger.class));
    }

    @Test
    void createOrder_Scheduled_Success() {
        LocalDateTime scheduledTime = LocalDateTime.now().plusHours(1);
        orderCreateDto.setScheduledTime(scheduledTime);

        when(passengerMapper.selectById(1L)).thenReturn(passenger);
        when(pricingService.calculateEstimatedPrice(any())).thenReturn(estimatedPriceInfo);
        when(orderMapper.insert(any(Order.class))).thenReturn(1);

        OrderResponseDto response = orderService.createOrder(orderCreateDto);

        assertEquals(OrderStatus.SCHEDULED, response.getStatus());
        verify(orderEventProducer, never()).sendOrderCreatedEvent(any());
    }

    @Test
    void createOrder_Scheduled_TooSoon_ThrowsException() {
        LocalDateTime scheduledTime = LocalDateTime.now().plusMinutes(10);
        orderCreateDto.setScheduledTime(scheduledTime);

        when(passengerMapper.selectById(1L)).thenReturn(passenger);
        when(pricingService.calculateEstimatedPrice(any())).thenReturn(estimatedPriceInfo);

        assertThrows(OrderServiceException.class, () -> orderService.createOrder(orderCreateDto));
    }

    @Test
    void acceptOrder_Success() {
        Order order = new Order();
        order.setId(100L);
        order.setStatus(OrderStatus.PENDING_MATCH);

        Driver driver = new Driver();
        driver.setId(2L);
        driver.setAvailable(true);

        when(orderMapper.selectById(100L)).thenReturn(order);
        when(driverMapper.selectById(2L)).thenReturn(driver);

        orderService.acceptOrder(100L, 2L);

        assertEquals(OrderStatus.ACCEPTED, order.getStatus());
        assertEquals(driver.getId(), order.getDriverId());
        assertFalse(driver.isAvailable());
        verify(orderMapper).updateById(order);
        verify(driverMapper).updateById(driver);
    }

    @Test
    void acceptOrder_DriverNotAvailable_ThrowsException() {
        Order order = new Order();
        Driver driver = new Driver();
        driver.setAvailable(false);

        when(orderMapper.selectById(100L)).thenReturn(order);
        when(driverMapper.selectById(2L)).thenReturn(driver);

        assertThrows(OrderServiceException.class, () -> orderService.acceptOrder(100L, 2L));
    }

    @Test
    void cancelOrder_Success() {
        Order order = new Order();
        order.setId(100L);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setPassengerId(passenger.getId());
        Driver driver = new Driver();
        driver.setId(2L);
        order.setDriverId(driver.getId());

        when(orderMapper.selectById(100L)).thenReturn(order);
        when(driverMapper.selectById(2L)).thenReturn(driver);

        orderService.cancelOrder(100L);

        assertEquals(OrderStatus.CANCELED, order.getStatus());
        assertTrue(driver.isAvailable());
        verify(orderEventProducer).sendOrderStatusUpdateEvent(any());
        verify(orderMapper).updateById(order);
    }

    @Test
    void processDriverAssigned_Success() {
        Order order = new Order();
        order.setId(100L);
        order.setStatus(OrderStatus.PENDING_MATCH);
        order.setPassengerId(passenger.getId());

        Driver driver = new Driver();
        driver.setId(2L);
        driver.setAvailable(true);

        DriverAssignedEventDto event = new DriverAssignedEventDto();
        event.setOrderId(100L);
        event.setDriverId(2L);

        when(orderMapper.selectById(100L)).thenReturn(order);
        when(driverMapper.selectById(2L)).thenReturn(driver);

        orderService.processDriverAssigned(event);

        assertEquals(OrderStatus.DRIVER_ASSIGNED, order.getStatus());
        assertEquals(driver.getId(), order.getDriverId());
        assertFalse(driver.isAvailable());
        verify(orderEventProducer).sendOrderStatusUpdateEvent(any());
    }

    @Test
    void processDriverAssigned_OrderNotMatchable_Returns() {
        Order order = new Order();
        order.setStatus(OrderStatus.CANCELED);
        DriverAssignedEventDto event = new DriverAssignedEventDto();
        event.setOrderId(100L);

        when(orderMapper.selectById(100L)).thenReturn(order);

        orderService.processDriverAssigned(event);

        verify(driverMapper, never()).selectById(anyLong());
    }

    @Test
    void processOrderMatchFailed_Success() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_MATCH);

        when(orderMapper.selectById(100L)).thenReturn(order);

        orderService.processOrderMatchFailed(100L, "No drivers found");

        assertEquals(OrderStatus.FAILED, order.getStatus());
        verify(orderMapper).updateById(order);
    }

    @Test
    void processPaymentConfirmation_Success() {
        Order order = new Order();
        order.setId(100L);
        order.setStatus(OrderStatus.COMPLETED);
        order.setPassengerId(passenger.getId());
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        order.setDriverAssignedTime(LocalDateTime.now().minusMinutes(30));
        order.setOrderTime(LocalDateTime.now().minusMinutes(30)); // Mocking order completion

        when(orderMapper.selectById(100L)).thenReturn(order);
        FinalPriceInfo finalPrice = new FinalPriceInfo();
        finalPrice.setFinalCost(3000L);
        finalPrice.setActualDistance(5.0);
        finalPrice.setActualDuration(15L);
        when(pricingService.calculateFinalPrice(any(), any())).thenReturn(finalPrice);

        orderService.processPaymentConfirmation(100L, 30.0, "TX123");

        assertEquals(OrderStatus.PAYMENT_SETTLED, order.getStatus());
        verify(orderEventProducer).sendOrderPaymentSettledEvent(any());
    }

    @Test
    void processDriverAssigned_DriverNotAvailable_ReturnsPendingMatch() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_MATCH);
        Driver driver = new Driver();
        driver.setAvailable(false);
        DriverAssignedEventDto event = new DriverAssignedEventDto();
        event.setOrderId(100L);
        event.setDriverId(2L);

        when(orderMapper.selectById(100L)).thenReturn(order);
        when(driverMapper.selectById(2L)).thenReturn(driver);

        orderService.processDriverAssigned(event);

        assertEquals(OrderStatus.PENDING_MATCH, order.getStatus());
    }

    @Test
    void processOrderMatchFailed_OrderNotFound_DoesNothing() {
        when(orderMapper.selectById(100L)).thenReturn(null);
        orderService.processOrderMatchFailed(100L, "Reason");
        verify(orderMapper, never()).updateById(any(Order.class));
    }

    @Test
    void processPaymentConfirmation_AlreadySettled_Success() {
        Order order = new Order();
        order.setStatus(OrderStatus.PAYMENT_SETTLED);
        order.setPassengerId(passenger.getId());
        order.setDriverId(2L);
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        when(orderMapper.selectById(100L)).thenReturn(order);
        FinalPriceInfo finalPrice = new FinalPriceInfo();
        finalPrice.setFinalCost(3000L);
        finalPrice.setActualDistance(5.0);
        finalPrice.setActualDuration(15L);
        when(pricingService.calculateFinalPrice(any(), any())).thenReturn(finalPrice);

        orderService.processPaymentConfirmation(100L, 30.0, "TX123");

        assertEquals(OrderStatus.PAYMENT_SETTLED, order.getStatus());
    }

    @Test
    void processPaymentConfirmation_OrderNotCompleted_DoesNothing() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_MATCH);
        when(orderMapper.selectById(100L)).thenReturn(order);
        orderService.processPaymentConfirmation(100L, 30.0, "TX123");
        verify(orderMapper, times(1)).selectById(100L);
        verify(orderMapper, never()).updateById(any(Order.class));
    }
}
