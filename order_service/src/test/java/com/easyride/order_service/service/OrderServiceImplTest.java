package com.easyride.order_service.service;

import com.easyride.order_service.dto.*;
import com.easyride.order_service.exception.OrderServiceException;
import com.easyride.order_service.exception.ResourceNotFoundException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PassengerRepository passengerRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private UserRepository userRepository;
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
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(pricingService.calculateEstimatedPrice(any())).thenReturn(estimatedPriceInfo);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(100L);
            return o;
        });

        OrderResponseDto response = orderService.createOrder(orderCreateDto);

        assertNotNull(response);
        assertEquals(100L, response.getOrderId());
        assertEquals(OrderStatus.PENDING_MATCH, response.getStatus());
        verify(orderEventProducer, times(1)).sendOrderCreatedEvent(any());
        verify(orderRepository, times(1)).save(any());
    }

    @Test
    void createOrder_PassengerNotFound_CreatesPlaceholder() {
        when(passengerRepository.findById(1L)).thenReturn(Optional.empty());
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pricingService.calculateEstimatedPrice(any())).thenReturn(estimatedPriceInfo);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(100L);
            return o;
        });

        orderService.createOrder(orderCreateDto);

        verify(passengerRepository, times(1)).save(any(Passenger.class));
    }

    @Test
    void createOrder_Scheduled_Success() {
        LocalDateTime scheduledTime = LocalDateTime.now().plusHours(1);
        orderCreateDto.setScheduledTime(scheduledTime);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(pricingService.calculateEstimatedPrice(any())).thenReturn(estimatedPriceInfo);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(100L);
            return o;
        });

        OrderResponseDto response = orderService.createOrder(orderCreateDto);

        assertEquals(OrderStatus.SCHEDULED, response.getStatus());
        verify(orderEventProducer, never()).sendOrderCreatedEvent(any());
    }

    @Test
    void createOrder_Scheduled_TooSoon_ThrowsException() {
        LocalDateTime scheduledTime = LocalDateTime.now().plusMinutes(10);
        orderCreateDto.setScheduledTime(scheduledTime);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
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

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(driverRepository.findById(2L)).thenReturn(Optional.of(driver));

        orderService.acceptOrder(100L, 2L);

        assertEquals(OrderStatus.ACCEPTED, order.getStatus());
        assertEquals(driver, order.getDriver());
        assertFalse(driver.isAvailable());
        verify(orderRepository).save(order);
        verify(driverRepository).save(driver);
    }

    @Test
    void acceptOrder_DriverNotAvailable_ThrowsException() {
        Order order = new Order();
        Driver driver = new Driver();
        driver.setAvailable(false);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(driverRepository.findById(2L)).thenReturn(Optional.of(driver));

        assertThrows(OrderServiceException.class, () -> orderService.acceptOrder(100L, 2L));
    }

    @Test
    void cancelOrder_Success() {
        Order order = new Order();
        order.setId(100L);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setPassenger(passenger);
        Driver driver = new Driver();
        driver.setId(2L);
        order.setDriver(driver);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        orderService.cancelOrder(100L);

        assertEquals(OrderStatus.CANCELED, order.getStatus());
        assertTrue(driver.isAvailable());
        verify(orderEventProducer).sendOrderStatusUpdateEvent(any());
    }

    @Test
    void processDriverAssigned_Success() {
        Order order = new Order();
        order.setId(100L);
        order.setStatus(OrderStatus.PENDING_MATCH);
        order.setPassenger(passenger);

        Driver driver = new Driver();
        driver.setId(2L);
        driver.setAvailable(true);

        DriverAssignedEventDto event = new DriverAssignedEventDto();
        event.setOrderId(100L);
        event.setDriverId(2L);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(driverRepository.findById(2L)).thenReturn(Optional.of(driver));

        orderService.processDriverAssigned(event);

        assertEquals(OrderStatus.DRIVER_ASSIGNED, order.getStatus());
        assertEquals(driver, order.getDriver());
        assertFalse(driver.isAvailable());
        verify(orderEventProducer).sendOrderStatusUpdateEvent(any());
    }

    @Test
    void processDriverAssigned_OrderNotMatchable_Returns() {
        Order order = new Order();
        order.setStatus(OrderStatus.CANCELED);
        DriverAssignedEventDto event = new DriverAssignedEventDto();
        event.setOrderId(100L);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        orderService.processDriverAssigned(event);

        verify(driverRepository, never()).findById(anyLong());
    }

    @Test
    void processOrderMatchFailed_Success() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_MATCH);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        orderService.processOrderMatchFailed(100L, "No drivers found");

        assertEquals(OrderStatus.FAILED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void processPaymentConfirmation_Success() {
        Order order = new Order();
        order.setId(100L);
        order.setStatus(OrderStatus.COMPLETED);
        order.setPassenger(passenger);
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        order.setDriverAssignedTime(LocalDateTime.now().minusMinutes(30));
        order.setOrderTime(LocalDateTime.now().minusMinutes(30)); // Mocking order completion

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
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

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(driverRepository.findById(2L)).thenReturn(Optional.of(driver));

        orderService.processDriverAssigned(event);

        assertEquals(OrderStatus.PENDING_MATCH, order.getStatus());
    }

    @Test
    void processOrderMatchFailed_OrderNotFound_DoesNothing() {
        when(orderRepository.findById(100L)).thenReturn(Optional.empty());
        orderService.processOrderMatchFailed(100L, "Reason");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void processPaymentConfirmation_AlreadySettled_Success() {
        Order order = new Order();
        order.setStatus(OrderStatus.PAYMENT_SETTLED);
        order.setPassenger(passenger);
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        order.setDriverAssignedTime(LocalDateTime.now());
        order.setOrderTime(LocalDateTime.now());

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
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
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        orderService.processPaymentConfirmation(100L, 30.0, "TX123");
        verify(orderRepository, times(1)).findById(100L);
        verify(orderRepository, never()).save(any());
    }
}
