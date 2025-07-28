package com.easyride.order_service.service;

import com.easyride.order_service.dto.*;
import com.easyride.order_service.exception.OrderServiceException;
import com.easyride.order_service.exception.ResourceNotFoundException;
import com.easyride.order_service.model.*;
import com.easyride.order_service.repository.DriverRepository;
import com.easyride.order_service.repository.OrderRepository;
import com.easyride.order_service.repository.PassengerRepository;
import com.easyride.order_service.repository.UserRepository;
import com.easyride.order_service.rocket.OrderEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class) 启动 Mockito 框架
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    // @Mock 会为所有标记的字段创建模拟对象
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PassengerRepository passengerRepository;
    @Mock
    private DriverRepository driverRepository; // 虽然本次测试用不到，但最好也声明
    @Mock
    private UserRepository userRepository;     // 同上
    @Mock
    private PricingService pricingService;
    @Mock
    private OrderEventProducer orderEventProducer;

    // @InjectMocks 会创建一个 OrderServiceImpl 的实例，
    // 并将上面所有 @Mock 标记的模拟对象自动注入到这个实例中。
    @InjectMocks
    private OrderServiceImpl orderServiceImpl;

    private OrderCreateDto orderCreateDto;
    private Passenger passenger;
    private EstimatedPriceInfo estimatedPriceInfo;

    // @BeforeEach 方法会在每个测试方法运行前执行，用于准备通用的测试数据
    @BeforeEach
    void setUp() {
        // 准备一个合法的乘客输入
        passenger = new Passenger();
        passenger.setId(1L);
        passenger.setUsername("Test Passenger");

        // 准备一个合法的订单创建DTO
        orderCreateDto = new OrderCreateDto();
        orderCreateDto.setPassengerId(1L);
        orderCreateDto.setStartLocation(new LocationDto(34.0, -118.0));
        orderCreateDto.setEndLocation(new LocationDto(34.1, -118.1));
        orderCreateDto.setVehicleType(VehicleType.STANDARD);
        orderCreateDto.setServiceType(ServiceType.NORMAL);
        orderCreateDto.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        orderCreateDto.setScheduledTime(null); // 测试即时订单

        // 准备一个模拟的计价服务返回结果
        estimatedPriceInfo = EstimatedPriceInfo.builder()
                .estimatedCost(50.0)
                .estimatedDistance(10.0)
                .estimatedDuration(20.0)
                .build();
    }

    @Test
    void createOrder_whenImmediateOrder_shouldSaveAndPublishEvent() {
        // 1. 准备 (Arrange) - 定义模拟对象的行为
        // 当 passengerRepository.findById 被调用时，返回我们准备好的乘客对象
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        // 当 pricingService.calculateEstimatedPrice 被调用时，返回准备好的价格信息
        when(pricingService.calculateEstimatedPrice(any(PricingContext.class))).thenReturn(estimatedPriceInfo);
        // 当 orderRepository.save 被调用时，直接返回传入的 Order 对象
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. 执行 (Act) - 调用我们要测试的方法
        OrderResponseDto responseDto = orderServiceImpl.createOrder(orderCreateDto);

        // 3. 断言 (Assert) - 验证结果和交互是否符合预期

        // 3.1 验证返回的 DTO 是否正确
        assertNotNull(responseDto);

        // 3.2 验证与数据库的交互
        // ArgumentCaptor 是一个强大的工具，可以捕获传递给 mock 方法的参数
        ArgumentCaptor<Order> orderArgumentCaptor = ArgumentCaptor.forClass(Order.class);

        // verify 检查 orderRepository.save 方法是否被【恰好调用了1次】
        // .capture() 会捕获那次调用时传入的 Order 对象
        verify(orderRepository, times(1)).save(orderArgumentCaptor.capture());

        // 获取被捕获的 Order 对象，进行详细断言
        Order savedOrder = orderArgumentCaptor.getValue();
        assertNotNull(savedOrder);
        assertEquals(passenger, savedOrder.getPassenger());
        assertEquals(OrderStatus.PENDING_MATCH, savedOrder.getStatus()); // 关键：即时订单的状态应该是 PENDING_MATCH
        assertEquals(50.0, savedOrder.getEstimatedCost());
        assertEquals(10.0, savedOrder.getEstimatedDistance());
        assertEquals(20.0, savedOrder.getEstimatedDuration());
        assertNull(savedOrder.getScheduledTime()); // 关键：即时订单没有预约时间

        // 3.3 验证与消息队列的交互
        // 验证 orderEventProducer.sendOrderCreatedEvent 方法是否被【恰好调用了1次】
        verify(orderEventProducer, times(1)).sendOrderCreatedEvent(any(OrderCreatedEvent.class));
    }
    // ... 此前的 @Mock, @InjectMocks, setUp() 等代码保持不变 ...

    // 【边缘情况1】: 测试创建预约订单 (Scheduled Order)
    @Test
    void createOrder_whenScheduledOrder_shouldSaveAsScheduledAndNotPublishEvent() {
        // 1. 准备 (Arrange)
        // 创建一个包含未来预约时间的 DTO
        orderCreateDto.setScheduledTime(LocalDateTime.now().plusHours(1));

        // 定义模拟对象的行为
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(pricingService.calculateEstimatedPrice(any(PricingContext.class))).thenReturn(estimatedPriceInfo);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. 执行 (Act)
        orderServiceImpl.createOrder(orderCreateDto);

        // 3. 断言 (Assert)
        // 捕获传递给 orderRepository.save 的 Order 对象
        ArgumentCaptor<Order> orderArgumentCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderArgumentCaptor.capture());

        Order savedOrder = orderArgumentCaptor.getValue();
        assertNotNull(savedOrder);
        // 关键断言：预约订单的初始状态应该是 SCHEDULED
        assertEquals(OrderStatus.SCHEDULED, savedOrder.getStatus());
        assertNotNull(savedOrder.getScheduledTime());

        // 关键断言：对于预约订单，不应该立即发送“订单已创建”的消息到匹配系统
        // verify(..., never()) 用来验证某个方法从未被调用过
        verify(orderEventProducer, never()).sendOrderCreatedEvent(any(OrderCreatedEvent.class));
    }


    // 【边缘情况2】: 测试乘客信息在本地不存在时，能自动创建占位信息
    @Test
    void createOrder_whenPassengerNotFound_shouldCreatePlaceholderPassenger() {
        // 1. 准备 (Arrange)
        // 模拟当根据id查找乘客时，返回空结果
        when(passengerRepository.findById(1L)).thenReturn(Optional.empty());

        // 模拟保存新乘客时，返回一个设置好id的新乘客对象
        when(passengerRepository.save(any(Passenger.class))).thenAnswer(invocation -> {
            Passenger p = invocation.getArgument(0);
            p.setId(1L); // 模拟数据库生成了ID
            return p;
        });

        // 其他模拟行为保持不变
        when(pricingService.calculateEstimatedPrice(any(PricingContext.class))).thenReturn(estimatedPriceInfo);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. 执行 (Act)
        orderServiceImpl.createOrder(orderCreateDto);

        // 3. 断言 (Assert)
        // 验证 passengerRepository.save 方法是否被调用了一次，以保存那个临时的占位乘客
        verify(passengerRepository, times(1)).save(any(Passenger.class));

        // 验证 orderRepository.save 仍然被成功调用
        verify(orderRepository, times(1)).save(any(Order.class));
    }


    // 【边缘情况3】: 测试预约时间不合法（太近）时，应抛出异常
    @Test
    void createOrder_whenScheduledTimeIsTooSoon_shouldThrowException() {
        // 1. 准备 (Arrange)
        orderCreateDto.setScheduledTime(LocalDateTime.now().plusMinutes(10));

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));

        // 【关键修复】: 添加这一行！
        // 即使我们是在测试异常路径，也需要为之前的代码流程提供有效的模拟返回值。
        when(pricingService.calculateEstimatedPrice(any(PricingContext.class))).thenReturn(estimatedPriceInfo);

        // 2. 执行 (Act) & 3. 断言 (Assert)
        OrderServiceException exception = assertThrows(OrderServiceException.class, () -> {
            orderServiceImpl.createOrder(orderCreateDto);
        });

        assertEquals("预约时间必须至少在当前时间15分钟后", exception.getMessage());

        verify(orderRepository, never()).save(any(Order.class));
        verify(orderEventProducer, never()).sendOrderCreatedEvent(any(OrderCreatedEvent.class));
    }
    @Test
    void cancelOrder_whenOrderExistsWithDriver_shouldUpdateStatusAndMakeDriverAvailableAndPublishEvent() {
        // 1. 准备 (Arrange)
        // 创建一个已分配司机的订单，这是测试的前提
        Driver assignedDriver = new Driver();
        assignedDriver.setId(202L);
        assignedDriver.setAvailable(false); // 司机当前是“不可用”状态

        Order existingOrder = new Order();
        existingOrder.setId(1L);
        existingOrder.setStatus(OrderStatus.DRIVER_ASSIGNED);
        existingOrder.setDriver(assignedDriver); // 订单关联了这位司机

        // 我们需要一个Passenger对象，即使它在此逻辑中不是主角
        Passenger passenger = new Passenger();
        passenger.setId(101L);
        existingOrder.setPassenger(passenger);

        // 模拟当根据ID查找订单时，返回这个已存在的订单
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existingOrder));

        // 2. 执行 (Act)
        // 调用我们要测试的 cancelOrder 方法
        orderServiceImpl.cancelOrder(1L);

        // 3. 断言 (Assert)

        // 3.1 验证订单状态是否被正确更新并保存
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(OrderStatus.CANCELED, savedOrder.getStatus()); // 关键断言：订单状态变为CANCELED

        // 3.2 验证司机的状态是否被更新并保存
        ArgumentCaptor<Driver> driverCaptor = ArgumentCaptor.forClass(Driver.class);
        verify(driverRepository, times(1)).save(driverCaptor.capture());
        Driver savedDriver = driverCaptor.getValue();
        assertTrue(savedDriver.isAvailable()); // 关键断言：司机的 available 状态变回 true

        // 3.3 验证是否发布了订单取消事件
        ArgumentCaptor<OrderEventDto> eventCaptor = ArgumentCaptor.forClass(OrderEventDto.class);
        verify(orderEventProducer, times(1)).sendOrderStatusUpdateEvent(eventCaptor.capture());
        OrderEventDto publishedEvent = eventCaptor.getValue();
        assertEquals(1L, publishedEvent.getOrderId());
        assertEquals(OrderStatus.CANCELED, publishedEvent.getStatus()); // 关键断言：发布的事件状态是 CANCELED
    }

    // --- 测试 cancelOrder 方法的边缘情况 ---
    @Test
    void cancelOrder_whenOrderNotFound_shouldThrowException() {
        // 1. 准备 (Arrange)
        // 模拟当根据一个不存在的ID查找订单时，返回空
        long nonExistentOrderId = 999L;
        when(orderRepository.findById(nonExistentOrderId)).thenReturn(Optional.empty());

        // 2. 执行 & 断言
        // 断言调用 cancelOrder 会抛出 ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () -> {
            orderServiceImpl.cancelOrder(nonExistentOrderId);
        });

        // 验证：因为订单未找到，后续的任何保存或消息发送操作都不应该发生
        verify(orderRepository, never()).save(any(Order.class));
        verify(driverRepository, never()).save(any(Driver.class));
        verify(orderEventProducer, never()).sendOrderStatusUpdateEvent(any(OrderEventDto.class));
    }

    // --- 测试 processDriverAssigned 方法 ---
    @Test
    void processDriverAssigned_whenOrderIsPending_shouldAssignDriverAndUpdateStatus() {
        // 1. 准备 (Arrange)
        // 模拟一个来自匹配服务的“司机已分配”事件
        DriverAssignedEventDto event = new DriverAssignedEventDto(
                1L, 202L, "Test Driver", null, null, null, null
        );

        // 准备一个处于“等待匹配”状态的订单
        Order pendingOrder = new Order();
        pendingOrder.setId(1L);
        pendingOrder.setStatus(OrderStatus.PENDING_MATCH);
        // 此订单需要关联一个乘客
        pendingOrder.setPassenger(new Passenger());

        // 准备一个“可用”的司机
        Driver availableDriver = new Driver();
        availableDriver.setId(202L);
        availableDriver.setAvailable(true);

        // 定义模拟行为
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(driverRepository.findById(202L)).thenReturn(Optional.of(availableDriver));

        // 2. 执行 (Act)
        orderServiceImpl.processDriverAssigned(event);

        // 3. 断言 (Assert)

        // 3.1 验证订单是否被更新
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();

        assertEquals(OrderStatus.DRIVER_ASSIGNED, savedOrder.getStatus()); // 关键断言：订单状态更新
        assertNotNull(savedOrder.getDriver()); // 关键断言：订单已关联司机
        assertEquals(202L, savedOrder.getDriver().getId());

        // 3.2 验证司机状态是否被更新
        ArgumentCaptor<Driver> driverCaptor = ArgumentCaptor.forClass(Driver.class);
        verify(driverRepository, times(1)).save(driverCaptor.capture());
        Driver savedDriver = driverCaptor.getValue();
        assertFalse(savedDriver.isAvailable()); // 关键断言：司机变为“不可用”

        // 3.3 验证状态更新事件是否被发布
        verify(orderEventProducer, times(1)).sendOrderStatusUpdateEvent(any(OrderEventDto.class));
    }

    // --- 测试 processDriverAssigned 方法的边缘情况 ---
    @Test
    void processDriverAssigned_whenOrderNotPending_shouldDoNothing() {
        // 1. 准备 (Arrange)
        // 模拟一个“司机已分配”事件
        DriverAssignedEventDto event = new DriverAssignedEventDto(
                1L, 202L, "Test Driver", null, null, null, null
        );

        // 准备一个已经处于“已取消”状态的订单
        Order canceledOrder = new Order();
        canceledOrder.setId(1L);
        canceledOrder.setStatus(OrderStatus.CANCELED);
        canceledOrder.setPassenger(new Passenger());

        // 定义模拟行为：只返回这个已取消的订单
        when(orderRepository.findById(1L)).thenReturn(Optional.of(canceledOrder));

        // 2. 执行 (Act)
        orderServiceImpl.processDriverAssigned(event);

        // 3. 断言 (Assert)
        // 关键断言：因为订单状态不合法，任何保存操作（对订单或司机）和消息发送都不应该发生
        verify(orderRepository, never()).save(any(Order.class));
        verify(driverRepository, never()).findById(anyLong()); // 甚至都不应该去查找司机
        verify(driverRepository, never()).save(any(Driver.class));
        verify(orderEventProducer, never()).sendOrderStatusUpdateEvent(any(OrderEventDto.class));
    }
    // --- 测试“订单匹配失败”的业务逻辑 ---
    @Test
    void processOrderMatchFailed_whenOrderIsPending_shouldUpdateStatusToFailed() {
        // 1. 准备 (Arrange)
        long orderId = 1L;
        String reason = "No drivers available in the area";

        // 准备一个处于“等待匹配”状态的订单
        Order pendingOrder = new Order();
        pendingOrder.setId(orderId);
        pendingOrder.setStatus(OrderStatus.PENDING_MATCH);

        // 模拟当根据ID查找订单时，返回这个订单
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));

        // 2. 执行 (Act)
        orderServiceImpl.processOrderMatchFailed(orderId, reason);

        // 3. 断言 (Assert)
        // 验证订单是否被保存，并且状态已更新为 FAILED
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertEquals(OrderStatus.FAILED, savedOrder.getStatus());
    }

    // --- 测试“支付已确认”的业务逻辑 ---
    @Test
    void processPaymentConfirmation_whenOrderIsCompleted_shouldSettlePaymentAndPublishEvent() {
        // 1. 准备 (Arrange)
        long orderId = 1L;
        double finalAmount = 99.8;
        String transactionId = "TXN123456789";

        // 准备一个处于“已完成”状态的订单
        Order completedOrder = new Order();
        completedOrder.setId(orderId);
        completedOrder.setStatus(OrderStatus.COMPLETED);
        completedOrder.setPassenger(new Passenger()); // 支付事件需要乘客信息
        completedOrder.setDriver(new Driver());     // 支付事件需要司机信息
        completedOrder.setPaymentMethod(PaymentMethod.ONLINE_PAYMENT); // 设置支付方式
        // 为价格计算设置必要的地理位置信息
        completedOrder.setStartLatitude(1.0);
        completedOrder.setStartLongitude(1.0);
        completedOrder.setEndLatitude(2.0);
        completedOrder.setEndLongitude(2.0);

        // 准备一个模拟的最终价格计算结果
        FinalPriceInfo finalPriceInfo = FinalPriceInfo.builder()
                .finalCost(99L) // 注意这里是Long
                .actualDistance(15.0)
                .actualDuration(30L) // 注意这里是Long
                .build();

        // 模拟依赖行为
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(completedOrder));
        when(pricingService.calculateFinalPrice(any(Order.class), any(PricingContext.class))).thenReturn(finalPriceInfo);

        // 2. 执行 (Act)
        orderServiceImpl.processPaymentConfirmation(orderId, finalAmount, transactionId);

        // 3. 断言 (Assert)
        // 验证订单是否被保存，并且状态和最终价格信息已更新
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(OrderStatus.PAYMENT_SETTLED, savedOrder.getStatus());
        assertEquals(99.0, savedOrder.getFinalCost()); // 验证最终价格
        assertEquals(15.0, savedOrder.getActualDistance()); // 验证最终距离

        // 验证“支付已结算”事件是否被发布
        verify(orderEventProducer, times(1)).sendOrderPaymentSettledEvent(any(OrderPaymentSettledEvent.class));
    }
}