package com.easyride.order_service.service;

import com.easyride.order_service.dto.*;
import com.easyride.order_service.model.*;
import com.easyride.order_service.repository.*;
import com.easyride.order_service.util.DistanceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

//createOrder 方法负责处理乘客下单的业务逻辑，包括预估费用、距离、时间等。
//acceptOrder 方法负责司机接单的业务逻辑，更新订单和司机的状态。
//updateOrderStatus 方法用于更新订单状态，例如司机已到达、行程中、已完成等，并在订单完成或取消时释放司机的可用状态。
//getOrderById 方法用于获取订单的详细信息。

@Service
public  class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            PassengerRepository passengerRepository,
                            DriverRepository driverRepository) {
        this.orderRepository = orderRepository;
        this.passengerRepository = passengerRepository;
        this.driverRepository = driverRepository;
    }

    @Override
    public OrderResponseDto createOrder(OrderRequestDto orderRequest) {

        Passenger passenger = passengerRepository.findById(orderRequest.getPassengerId())
                .orElseThrow(() -> new RuntimeException("乘客不存在"));

        // 预估费用、距离和时间（这里简单模拟）
        Double estimatedCost = estimateCost(orderRequest.getStartLocation(), orderRequest.getEndLocation(), orderRequest.getVehicleType());
        Double estimatedDistance = estimateDistance(orderRequest.getStartLocation(), orderRequest.getEndLocation());
        Double estimatedDuration = estimateDuration(estimatedDistance);

        Order order = new Order();
        order.setStatus("PENDING"); // 待接单
        order.setPassenger(passenger);
        order.setStartLocation(orderRequest.getStartLocation());
        order.setEndLocation(orderRequest.getEndLocation());
        order.setOrderTime(LocalDateTime.now());
        order.setEstimatedCost(estimatedCost);
        order.setEstimatedDistance(estimatedDistance);
        order.setEstimatedDuration(estimatedDuration);
        order.setVehicleType(orderRequest.getVehicleType());
        order.setServiceType(orderRequest.getServiceType());
        order.setPaymentMethod(orderRequest.getPaymentMethod());

        orderRepository.save(order);

        // 返回订单信息
        return mapToOrderResponseDto(order);
    }

    @Override
    public void acceptOrder(Long orderId, Long driverId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("司机不存在"));

        if (!driver.getIsAvailable()) {
            throw new RuntimeException("司机不空闲");
        }

        order.setDriver(driver);
        order.setStatus("ACCEPTED"); // 已接单
        orderRepository.save(order);

        // 更新司机状态
        driver.setIsAvailable(false);
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
        orderRepository.save(order);

        // 如果订单已分配司机，更新司机状态为可用
        if (order.getDriver() != null) {
            Driver driver = order.getDriver();
            driver.setAvailable(true);
            driverRepository.save(driver);
        }
    }

    @Override
    public void updateOrderStatus(Long orderId, String status) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        order.setStatus(status);
        orderRepository.save(order);

        // 如果订单完成或取消，释放司机
        if ("COMPLETED".equals(status) || "CANCELLED".equals(status)) {
            Driver driver = order.getDriver();
            if (driver != null) {
                driver.setIsAvailable(true);
                driverRepository.save(driver);
            }
        }
    }

    @Override
    public OrderResponseDto getOrderById(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        return mapToOrderResponseDto(order);
    }

    // 模拟预估费用计算
    private Double estimateCost(Location start, Location end, String vehicleType) {
        // 简单模拟，实际应根据距离、车型等计算
        return 50.0;
    }

    // 模拟距离计算
    private Double estimateDistance(Location start, Location end) {
        // 简单模拟，实际应计算两点间的距离
        return 10.0; // 单位：公里
    }

    // 模拟时间估算
    private Double estimateDuration(Double distance) {
        // 简单模拟，假设平均速度为40公里/小时
        return distance / 40 * 60; // 单位：分钟
    }

    private OrderResponseDto mapToOrderResponseDto(Order order) {
        return new OrderResponseDto(
                order.getId(),
                order.getStatus(),
                order.getPassenger().getId(),
                order.getDriver() != null ? order.getDriver().getId() : null,
                order.getStartLocation(),
                order.getEndLocation(),
                order.getOrderTime(),
                order.getEstimatedCost(),
                order.getEstimatedDistance(),
                order.getEstimatedDuration(),
                order.getVehicleType(),
                order.getServiceType(),
                order.getPaymentMethod()
        );
    }
}
