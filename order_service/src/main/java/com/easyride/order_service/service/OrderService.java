package com.easyride.order_service.service;

import com.easyride.order_service.dto.*;
import com.easyride.order_service.model.OrderStatus;

public interface OrderService {

    // 创建订单
    OrderResponseDto createOrder(OrderCreateDto orderCreateDto);

    // 司机接受订单
    void acceptOrder(Long orderId, Long driverId);

    // 获取订单详情
    OrderResponseDto getOrderDetails(Long orderId);

    // 取消订单
    void cancelOrder(Long orderId);

    // 更新订单状态
    void updateOrderStatus(Long orderId, OrderStatus status);

    // 其他订单相关的方法
}

