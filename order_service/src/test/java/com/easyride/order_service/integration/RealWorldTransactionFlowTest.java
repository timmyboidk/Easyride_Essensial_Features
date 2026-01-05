package com.easyride.order_service.integration;

import com.easyride.order_service.dto.*;
import com.easyride.order_service.repository.*;
import com.easyride.order_service.model.*;
import com.easyride.order_service.rocket.OrderEventProducer;
import com.easyride.order_service.service.OrderServiceImpl;
import com.easyride.order_service.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealWorldTransactionFlowTest {

    @Mock
    private OrderMapper orderMapper; // Changed from OrderRepository to OrderMapper
    @Mock
    private PassengerMapper passengerMapper; // Changed from PassengerRepository to PassengerMapper
    @Mock
    private DriverMapper driverMapper; // Changed from DriverRepository to DriverMapper
    @Mock
    private UserMapper userMapper; // Changed from UserRepository to UserMapper
    @Mock
    private PricingService pricingService;
    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderServiceImpl orderService;

    private AtomicReference<Order> orderStore = new AtomicReference<>();
    private AtomicReference<Driver> driverStore = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        // Simulate DB persistence for Order (Insert)
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(1001L);
            }
            orderStore.set(order);
            return 1; // Mappers typically return affected rows count
        });

        // Simulate DB persistence for Order (Update)
        lenient().when(orderMapper.updateById(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            orderStore.set(order);
            return 1; // Mappers typically return affected rows count
        });

        when(orderMapper.selectById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            if (orderStore.get() != null && orderStore.get().getId().equals(id)) {
                return orderStore.get(); // Mappers typically return the entity directly or null
            }
            return null;
        });

        // Simulate DB persistence for Driver (Update)
        // Since test doesn't explicitly save Driver via service (service logic for
        // accept might, but we mock mapper)
        // Actually OrderService might update driver availability using DriverMapper.
        lenient().when(driverMapper.updateById(any(Driver.class))).thenAnswer(invocation -> {
            Driver driver = invocation.getArgument(0);
            driverStore.set(driver);
            return 1; // Mappers typically return affected rows count
        });

        // Mock Driver retrieval
        when(driverMapper.selectById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            if (id == 2002L) { // Driver ID
                if (driverStore.get() == null) {
                    Driver d = new Driver();
                    d.setId(id);
                    d.setAvailable(true);
                    driverStore.set(d);
                }
                return driverStore.get(); // Mappers typically return the entity directly or null
            }
            return null;
        });

        // Mock Passenger
        when(passengerMapper.selectById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            Passenger p = new Passenger();
            p.setId(id);
            p.setUsername("Passenger_" + id);
            return p; // Mappers typically return the entity directly or null
        });

        // Mock Pricing
        when(pricingService.calculateEstimatedPrice(any())).thenReturn(
                new EstimatedPriceInfo(25.0, 5.0, 15.0, "USD", "{}"));
        when(pricingService.calculateFinalPrice(any(), any())).thenReturn(
                FinalPriceInfo.builder()
                        .finalCost(25L)
                        .actualDistance(5.0)
                        .actualDuration(15L)
                        .baseFare(10L)
                        .distanceCost(10L)
                        .timeCost(5L)
                        .build());
    }

    @Test
    void testFullRideLifecycle() {
        // 1. Create Order
        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setPassengerId(101L);
        createDto.setStartLocation(new LocationDto(37.7749, -122.4194));
        createDto.setEndLocation(new LocationDto(37.8044, -122.2711));
        createDto.setVehicleType(VehicleType.ECONOMY);
        createDto.setServiceType(ServiceType.NORMAL);
        createDto.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        OrderResponseDto createdOrder = orderService.createOrder(createDto);

        assertNotNull(createdOrder);
        assertEquals(OrderStatus.PENDING_MATCH, createdOrder.getStatus());
        assertEquals(101L, orderStore.get().getPassengerId());

        System.out.println("Step 1: Order Created. Status: " + createdOrder.getStatus());

        // 2. Assign Driver (Simulate Matching Service)
        // Constructor: orderId, driverId, name, plate, model, rating, time
        DriverAssignedEventDto assignedEvent = new DriverAssignedEventDto(
                createdOrder.getOrderId(),
                2002L,
                "Driver John",
                "ABC-123",
                "Toyota Prius",
                4.8,
                LocalDateTime.now().plusMinutes(5));

        orderService.processDriverAssigned(assignedEvent);

        assertEquals(OrderStatus.DRIVER_ASSIGNED, orderStore.get().getStatus());
        assertEquals(2002L, orderStore.get().getDriverId());
        assertFalse(driverStore.get().isAvailable()); // Driver should be busy

        System.out.println("Step 2: Driver Assigned. Status: " + orderStore.get().getStatus());

        // 3. Driver Accepts Order
        orderService.acceptOrder(createdOrder.getOrderId(), 2002L);

        assertEquals(OrderStatus.ACCEPTED, orderStore.get().getStatus());

        System.out.println("Step 3: Driver Accepted. Status: " + orderStore.get().getStatus());

        // 4. Driver Arrived
        orderService.updateOrderStatus(createdOrder.getOrderId(), OrderStatus.ARRIVED);

        assertEquals(OrderStatus.ARRIVED, orderStore.get().getStatus());

        System.out.println("Step 4: Driver Arrived. Status: " + orderStore.get().getStatus());

        // 5. Trip Start
        orderService.updateOrderStatus(createdOrder.getOrderId(), OrderStatus.IN_PROGRESS);

        assertEquals(OrderStatus.IN_PROGRESS, orderStore.get().getStatus());

        System.out.println("Step 5: Trip In Progress. Status: " + orderStore.get().getStatus());

        // 6. Trip End (Completed)
        orderService.updateOrderStatus(createdOrder.getOrderId(), OrderStatus.COMPLETED);

        assertEquals(OrderStatus.COMPLETED, orderStore.get().getStatus());

        System.out.println("Step 6: Trip Completed. Status: " + orderStore.get().getStatus());

        // 7. Payment Confirmation
        orderService.processPaymentConfirmation(createdOrder.getOrderId(), 25.0, "TXN_12345");

        assertEquals(OrderStatus.PAYMENT_SETTLED, orderStore.get().getStatus());
        assertEquals(25.0, orderStore.get().getFinalCost());

        System.out.println("Step 7: Payment Settled. Status: " + orderStore.get().getStatus());

        // Verify Events were produced
        verify(orderEventProducer, times(1)).sendOrderCreatedEvent(any());
        verify(orderEventProducer, atLeastOnce()).sendOrderStatusUpdateEvent(any());
        verify(orderEventProducer, times(1)).sendOrderPaymentSettledEvent(any());
    }
}
