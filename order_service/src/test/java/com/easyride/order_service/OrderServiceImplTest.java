package com.easyride.order_service.service;

import com.easyride.order_service.dto.*;
import com.easyride.order_service.model.*;
import com.easyride.order_service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // 测试：乘客不存在
    @Test
    void testCreateOrder_PassengerNotFound() {
        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setPassengerId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(createDto);
        });
        assertEquals("乘客不存在", exception.getMessage());
    }

    // 测试：用户角色不是乘客
    @Test
    void testCreateOrder_InvalidPassengerRole() {
        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setPassengerId(1L);

        User user = new User();
        user.setId(1L);
        user.setUsername("TestUser");
        user.setRole("DRIVER");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(createDto);
        });
        assertEquals("用户不是乘客", exception.getMessage());
    }

    // 测试：没有可用的司机
    @Test
    void testCreateOrder_NoAvailableDriver() {
        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setPassengerId(1L);
        createDto.setStartLocation(new LocationDto(10.0, 20.0));
        createDto.setEndLocation(new LocationDto(30.0, 40.0));
        createDto.setVehicleType(VehicleType.CAR);
        createDto.setServiceType(ServiceType.STANDARD);
        createDto.setPaymentMethod(PaymentMethod.CASH);

        User user = new User();
        user.setId(1L);
        user.setUsername("Passenger");
        user.setRole("PASSENGER");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Passenger passenger = new Passenger();
        passenger.setId(1L);
        passenger.setName("Passenger");
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));

        when(driverRepository.findFirstByVehicleTypeAndAvailableTrue(VehicleType.CAR)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(createDto);
        });
        assertEquals("没有可用的司机", exception.getMessage());
    }

    // 测试：创建订单成功
    @Test
    void testCreateOrder_Success() {
        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setPassengerId(1L);
        createDto.setStartLocation(new LocationDto(10.0, 20.0));
        createDto.setEndLocation(new LocationDto(30.0, 40.0));
        createDto.setVehicleType(VehicleType.CAR);
        createDto.setServiceType(ServiceType.STANDARD);
        createDto.setPaymentMethod(PaymentMethod.CASH);

        User user = new User();
        user.setId(1L);
        user.setUsername("Passenger");
        user.setRole("PASSENGER");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Passenger passenger = new Passenger();
        passenger.setId(1L);
        passenger.setName("Passenger");
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));

        Driver driver = new Driver();
        driver.setId(10L);
        driver.setName("Driver");
        driver.setAvailable(true);
        when(driverRepository.findFirstByVehicleTypeAndAvailableTrue(VehicleType.CAR)).thenReturn(Optional.of(driver));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });

        OrderResponseDto response = orderService.createOrder(createDto);
        assertNotNull(response);
        assertEquals(100L, response.getOrderId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals("Passenger", response.getPassengerName());
        assertEquals("Driver", response.getDriverName());
        // 验证司机状态已更新为不可用
        assertFalse(driver.isAvailable());
        verify(driverRepository).save(driver);
    }

    // 测试：司机接单时订单不存在
    @Test
    void testAcceptOrder_OrderNotFound() {
        when(orderRepository.findById(200L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.acceptOrder(200L, 10L);
        });
        assertEquals("订单不存在", exception.getMessage());
    }

    // 测试：司机接单时司机不存在
    @Test
    void testAcceptOrder_DriverNotFound() {
        Order order = new Order();
        order.setId(200L);
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));

        when(driverRepository.findById(10L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.acceptOrder(200L, 10L);
        });
        assertEquals("司机不存在", exception.getMessage());
    }

    // 测试：司机接单时司机不可用
    @Test
    void testAcceptOrder_DriverNotAvailable() {
        Order order = new Order();
        order.setId(200L);
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));

        Driver driver = new Driver();
        driver.setId(10L);
        driver.setAvailable(false);
        when(driverRepository.findById(10L)).thenReturn(Optional.of(driver));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.acceptOrder(200L, 10L);
        });
        assertEquals("司机不可用", exception.getMessage());
    }

    // 测试：司机接单成功
    @Test
    void testAcceptOrder_Success() {
        Order order = new Order();
        order.setId(200L);
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));

        Driver driver = new Driver();
        driver.setId(10L);
        driver.setAvailable(true);
        when(driverRepository.findById(10L)).thenReturn(Optional.of(driver));

        orderService.acceptOrder(200L, 10L);

        assertEquals(OrderStatus.ACCEPTED, order.getStatus());
        verify(orderRepository).save(order);
        verify(driverRepository).save(driver);
        assertFalse(driver.isAvailable());
    }

    // 测试：查询订单详情时订单不存在
    @Test
    void testGetOrderDetails_OrderNotFound() {
        when(orderRepository.findById(300L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.getOrderDetails(300L);
        });
        assertEquals("订单不存在", exception.getMessage());
    }

    // 测试：查询订单详情成功
    @Test
    void testGetOrderDetails_Success() {
        Order order = new Order();
        order.setId(300L);
        order.setStatus(OrderStatus.PENDING);
        Passenger passenger = new Passenger();
        passenger.setId(1L);
        passenger.setName("Passenger");
        order.setPassenger(passenger);

        Driver driver = new Driver();
        driver.setId(10L);
        driver.setName("Driver");
        order.setDriver(driver);

        order.setEstimatedCost(120.0);
        order.setEstimatedDistance(50.0);
        order.setEstimatedDuration(30.0);

        when(orderRepository.findById(300L)).thenReturn(Optional.of(order));

        OrderResponseDto response = orderService.getOrderDetails(300L);
        assertNotNull(response);
        assertEquals(300L, response.getOrderId());
        assertEquals("Passenger", response.getPassengerName());
        assertEquals("Driver", response.getDriverName());
    }

    // 测试：取消订单时订单不存在
    @Test
    void testCancelOrder_OrderNotFound() {
        when(orderRepository.findById(400L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.cancelOrder(400L);
        });
        assertEquals("订单不存在", exception.getMessage());
    }

    // 测试：取消订单成功（包括司机恢复可用状态）
    @Test
    void testCancelOrder_Success() {
        Order order = new Order();
        order.setId(400L);
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(400L)).thenReturn(Optional.of(order));

        // 情况1：订单无司机分配
        orderService.cancelOrder(400L);
        assertEquals(OrderStatus.CANCELED, order.getStatus());
        verify(orderRepository, times(1)).save(order);

        // 情况2：订单有司机分配，则司机状态恢复为可用
        Driver driver = new Driver();
        driver.setId(10L);
        driver.setAvailable(false);
        order.setDriver(driver);
        orderService.cancelOrder(400L);
        verify(driverRepository, times(1)).save(driver);
        assertTrue(driver.isAvailable());
    }

    // 测试：更新订单状态时订单不存在
    @Test
    void testUpdateOrderStatus_OrderNotFound() {
        when(orderRepository.findById(500L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.updateOrderStatus(500L, OrderStatus.CANCELED);
        });
        assertEquals("订单不存在", exception.getMessage());
    }

    // 测试：更新订单状态成功
    @Test
    void testUpdateOrderStatus_Success() {
        Order order = new Order();
        order.setId(500L);
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

        orderService.updateOrderStatus(500L, OrderStatus.ACCEPTED);
        assertEquals(OrderStatus.ACCEPTED, order.getStatus());
        verify(orderRepository).save(order);
    }
}
