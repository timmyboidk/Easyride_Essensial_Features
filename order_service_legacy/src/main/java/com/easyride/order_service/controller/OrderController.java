package com.easyride.order_service.controller;
import com.easyride.order_service.dto.OrderCreateDto;
import com.easyride.order_service.dto.OrderResponseDto;
import com.easyride.order_service.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

//OrderController 处理与订单相关的 HTTP 请求，包括创建订单、司机接单、更新订单状态和获取订单详情。
//使用 @RestController 和 @RequestMapping 注解定义控制器和路由。
@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // 乘客下单
    @PostMapping("/create")
    public ResponseEntity<OrderResponseDto> createOrder(@Validated @RequestBody OrderCreateDto orderRequest) {
        OrderResponseDto orderResponse = orderService.createOrder(orderRequest);
        return ResponseEntity.ok(orderResponse);
    }

    // 司机接单
    @PostMapping("/{orderId}/accept")
    public ResponseEntity<String> acceptOrder(@PathVariable Long orderId, @RequestParam Long driverId) {
        orderService.acceptOrder(orderId, driverId);
        return ResponseEntity.ok("订单已接单");
    }

    // 更新订单状态
    @PutMapping("/{orderId}/status")
    public ResponseEntity<String> updateOrderStatus(@PathVariable Long orderId, @RequestParam String status) {
        orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok("订单状态已更新");
    }

    // 获取订单详情
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long orderId) {
        OrderResponseDto orderResponse = orderService.getOrderById(orderId);
        return ResponseEntity.ok(orderResponse);
    }
}
