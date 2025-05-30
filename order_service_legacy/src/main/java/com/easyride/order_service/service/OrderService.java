package com.easyride.order_service.service;

import com.easyride.order_service.dto.*;

public interface OrderService {

    OrderResponseDto createOrder(OrderCreateDto orderCreateDto);

    void acceptOrder(Long orderId, Long driverId);

    // 新增方法，可以根据需求添加
    OrderResponseDto getOrderDetails(Long orderId);

    void cancelOrder(Long orderId);

    void updateOrderStatus(Long orderId, String status);

    OrderResponseDto getOrderById(Long orderId);

    // 其他方法
}
