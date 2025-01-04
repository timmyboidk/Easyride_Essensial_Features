package com.easyride.order_service.controller;

import com.easyride.order_service.dto.*;
import com.easyride.order_service.model.OrderStatus;
import com.easyride.order_service.service.OrderService;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    // 使用构造函数注入
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 创建订单
    @PostMapping("/create")
    public OrderResponseDto createOrder(@Valid @RequestBody OrderCreateDto orderCreateDto) {
        return orderService.createOrder(orderCreateDto);
    }

    // 司机接受订单
    @PostMapping("/{orderId}/accept")
    public void acceptOrder(@PathVariable Long orderId, @RequestParam Long driverId) {
        orderService.acceptOrder(orderId, driverId);
    }

    // 获取订单详情
    @GetMapping("/{orderId}")
    public OrderResponseDto getOrderDetails(@PathVariable Long orderId) {
        return orderService.getOrderDetails(orderId);
    }

    // 取消订单
    @PostMapping("/{orderId}/cancel")
    public void cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
    }

    // 更新订单状态
    @PostMapping("/{orderId}/status")
    public void updateOrderStatus(@PathVariable Long orderId, @RequestParam OrderStatus status) {
        orderService.updateOrderStatus(orderId, status);
    }
}
