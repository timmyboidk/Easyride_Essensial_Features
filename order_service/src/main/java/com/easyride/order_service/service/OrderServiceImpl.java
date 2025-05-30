package com.easyride.order_service.service;

import com.easyride.order_service.dto.OrderCreateDto;
import com.easyride.order_service.dto.OrderResponseDto;
import com.easyride.order_service.model.*;
import com.easyride.order_service.repository.*;
import com.easyride.order_service.util.DistanceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository; // 新增

    public OrderServiceImpl(OrderRepository orderRepository,
                            PassengerRepository passengerRepository,
                            DriverRepository driverRepository,
                            UserRepository userRepository) { // 更新构造函数
        this.orderRepository = orderRepository;
        this.passengerRepository = passengerRepository;
        this.driverRepository = driverRepository;
        this.userRepository = userRepository; // 初始化
    }

    @Override
    @Transactional
    public OrderResponseDto createOrder(OrderCreateDto orderCreateDto) {
        // 从本地数据库获取乘客信息
        User passengerUser = UserRepository.findById(orderCreateDto.getPassengerId())
                .orElseThrow(() -> new RuntimeException("乘客不存在"));

        if (!"PASSENGER".equalsIgnoreCase(passengerUser.getRole())) {
            throw new RuntimeException("用户不是乘客");
        }

        // 查找或创建乘客实体
        Passenger passenger = passengerRepository.findById(passengerUser.getId())
                .orElseGet(() -> {
                    Passenger newPassenger = new Passenger();
                    newPassenger.setId(passengerUser.getId());
                    newPassenger.setName(passengerUser.getUsername());
                    passengerRepository.save(newPassenger);
                    return newPassenger;
                });

        // 查找可用的司机
        Driver driver = driverRepository.findFirstByVehicleTypeAndAvailableTrue(orderCreateDto.getVehicleType())
                .orElseThrow(() -> new RuntimeException("没有可用的司机"));

        // 计算预估距离和费用
        double distance = DistanceCalculator.calculateDistance(
                orderCreateDto.getStartLocation().getLatitude(),
                orderCreateDto.getStartLocation().getLongitude(),
                orderCreateDto.getEndLocation().getLatitude(),
                orderCreateDto.getEndLocation().getLongitude()
        );

        double estimatedCost = distance * 2.0; // 简单的费用计算示例
        double estimatedDuration = distance / 50 * 60; // 假设平均速度为 50 km/h

        // 创建订单
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        order.setPassenger(passenger);
        order.setDriver(driver);
        order.setStartLatitude(orderCreateDto.getStartLocation().getLatitude());
        order.setStartLongitude(orderCreateDto.getStartLocation().getLongitude());
        order.setEndLatitude(orderCreateDto.getEndLocation().getLatitude());
        order.setEndLongitude(orderCreateDto.getEndLocation().getLongitude());
        order.setOrderTime(LocalDateTime.now());
        order.setEstimatedCost(estimatedCost);
        order.setEstimatedDistance(distance);
        order.setEstimatedDuration(estimatedDuration);
        order.setVehicleType(orderCreateDto.getVehicleType());
        order.setServiceType(orderCreateDto.getServiceType());
        order.setPaymentMethod(orderCreateDto.getPaymentMethod());
        order.setCreatedAt(LocalDateTime.now());
        order.setCreatedBy(passenger.getId());

        orderRepository.save(order);

        // 更新司机状态为不可用
        driver.setAvailable(false);
        driverRepository.save(driver);

        // 返回订单响应 DTO
        OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setOrderId(order.getId());
        responseDto.setStatus(order.getStatus());
        responseDto.setPassengerName(passenger.getName());
        responseDto.setDriverName(driver.getName());
        responseDto.setEstimatedCost(estimatedCost);
        responseDto.setEstimatedDistance(distance);
        responseDto.setEstimatedDuration(estimatedDuration);

        return responseDto;
    }

    @Override
    @Transactional
    public void acceptOrder(Long orderId, Long driverId) {
        // 查找订单
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 查找司机
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("司机不存在"));

        // 检查司机是否可用
        if (!driver.isAvailable()) {
            throw new RuntimeException("司机不可用");
        }

        // 更新订单状态并关联司机
        order.setStatus(OrderStatus.ACCEPTED);
        order.setDriver(driver);
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(driverId);
        orderRepository.save(order);

        // 更新司机状态为不可用
        driver.setAvailable(false);
        driverRepository.save(driver);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderDetails(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setOrderId(order.getId());
        responseDto.setStatus(order.getStatus());
        responseDto.setPassengerName(order.getPassenger().getName());
        responseDto.setDriverName(order.getDriver() != null ? order.getDriver().getName() : null);
        responseDto.setEstimatedCost(order.getEstimatedCost());
        responseDto.setEstimatedDistance(order.getEstimatedDistance());
        responseDto.setEstimatedDuration(order.getEstimatedDuration());

        return responseDto;
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        order.setStatus(OrderStatus.CANCELED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 如果订单已分配司机，更新司机状态为可用
        if (order.getDriver() != null) {
            Driver driver = order.getDriver();
            driver.setAvailable(true);
            driverRepository.save(driver);
        }
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }
}
