package com.easyride.order_service;

import com.easyride.order_service.dto.*;
import com.easyride.order_service.model.*;
import com.easyride.order_service.repository.*;
import com.easyride.order_service.service.OrderServiceImpl;
import com.easyride.order_service.util.DistanceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

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
        // 可以在此进行一些初始化操作
    }

    @Test
    void createOrder_Success() {
        // 准备测试数据
        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setPassengerId(1L);
        createDto.setStartLocation(new LocationDto(30.0, 120.0));
        createDto.setEndLocation(new LocationDto(31.0, 121.0));
        createDto.setVehicleType(VehicleType.valueOf("STANDARD"));
        createDto.setServiceType(ServiceType.valueOf("NORMAL"));
        createDto.setPaymentMethod(PaymentMethod.valueOf("CREDIT_CARD"));

        // 模拟从 userRepository 获取用户信息
        User passengerUser = new User();
        passengerUser.setId(1L);
        passengerUser.setUsername("TestPassenger");
        passengerUser.setRole("PASSENGER");

        when(UserRepository.findById(1L)).thenReturn(Optional.of(passengerUser));

        // 模拟 PassengerRepository
        Passenger passenger = new Passenger();
        passenger.setId(1L);
        passenger.setName("TestPassenger");
        when(passengerRepository.findById(1L)).thenReturn(Optional.empty()); // 表示本地还没存
        when(passengerRepository.save(any(Passenger.class))).thenReturn(passenger);

        // 模拟 DriverRepository
        Driver driver = new Driver();
        driver.setId(2L);
        driver.setName("TestDriver");
        driver.setAvailable(true);
        when(driverRepository.findFirstByVehicleTypeAndAvailableTrue(VehicleType.STANDARD))
                .thenReturn(Optional.of(driver));
        when(driverRepository.save(driver)).thenReturn(driver);

        // Mock 距离计算 (可选)
        // 这里默认不 stub DistanceCalculator，您可根据需要使用 spy 或 static mocking

        // 模拟 orderRepository
        Order savedOrder = new Order();
        savedOrder.setId(10L);
        savedOrder.setStatus(OrderStatus.PENDING);
        savedOrder.setPassenger(passenger);
        savedOrder.setDriver(driver);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order argument = invocation.getArgument(0);
            argument.setId(10L); // 模拟数据库生成ID
            return argument;
        });

        // 执行测试
        OrderResponseDto response = orderService.createOrder(createDto);

        // 验证结果
        assertNotNull(response);
        assertEquals(10L, response.getOrderId());
        assertEquals("PENDING", response.getStatus().name());
        assertEquals("TestPassenger", response.getPassengerName());
        assertEquals("TestDriver", response.getDriverName());

        // 验证交互
        verify(userRepository);
        UserRepository.findById(1L);
        verify(passengerRepository).save(any(Passenger.class));
        verify(driverRepository).findFirstByVehicleTypeAndAvailableTrue(VehicleType.STANDARD);
        verify(driverRepository).save(driver);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_NoAvailableDriver() {
        // 准备测试数据
        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setPassengerId(1L);
        createDto.setStartLocation(new LocationDto(30.0, 120.0));
        createDto.setEndLocation(new LocationDto(31.0, 121.0));
        createDto.setVehicleType(VehicleType.valueOf("STANDARD"));
        createDto.setServiceType(ServiceType.valueOf("NORMAL"));
        createDto.setPaymentMethod(PaymentMethod.valueOf("CREDIT_CARD"));

        // 模拟 userRepository
        User passengerUser = new User();
        passengerUser.setId(1L);
        passengerUser.setUsername("TestPassenger");
        passengerUser.setRole("PASSENGER");
        when(UserRepository.findById(1L)).thenReturn(Optional.of(passengerUser));

        // 模拟 driverRepository 返回空
        when(driverRepository.findFirstByVehicleTypeAndAvailableTrue(VehicleType.STANDARD))
                .thenReturn(Optional.empty());

        // 执行 & 验证异常
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.createOrder(createDto));
        assertEquals("没有可用的司机", exception.getMessage());
    }

    @Test
    void acceptOrder_Success() {
        // 准备
        Long orderId = 100L;
        Long driverId = 2L;

        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setStatus(OrderStatus.PENDING);

        Driver driver = new Driver();
        driver.setId(driverId);
        driver.setAvailable(true);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(orderRepository.save(any(Order.class))).thenReturn(existingOrder);

        // 执行
        orderService.acceptOrder(orderId, driverId);

        // 验证
        assertEquals(OrderStatus.ACCEPTED, existingOrder.getStatus());
        assertEquals(driver, existingOrder.getDriver());
        verify(orderRepository).save(existingOrder);
        verify(driverRepository).save(driver);
        assertFalse(driver.isAvailable());
    }

    @Test
    void acceptOrder_DriverUnavailable() {
        Long orderId = 100L;
        Long driverId = 2L;

        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setStatus(OrderStatus.PENDING);

        Driver driver = new Driver();
        driver.setId(driverId);
        driver.setAvailable(false); // 不可用

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.acceptOrder(orderId, driverId));
        assertEquals("司机不可用", exception.getMessage());
    }

    // 其他测试: getOrderDetails, cancelOrder, updateOrderStatus 同理模拟并断言
}
