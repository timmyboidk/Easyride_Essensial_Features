package com.easyride.order_service.controller;

import com.easyride.order_service.dto.*; // Ensure ApiResponse is here
import com.easyride.order_service.model.OrderStatus;
import com.easyride.order_service.security.OrderDetailsImpl;
import com.easyride.order_service.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public ApiResponse<OrderResponseDto> createOrder(@Valid @RequestBody OrderCreateDto orderCreateDto) {
        // Assuming passengerId comes from authenticated principal or is validated if passed in DTO
        // For simplicity, if passengerId is in DTO, ensure it matches authenticated user or requires specific role.
        // Long passengerId = getAuthenticatedPassengerId();
        // orderCreateDto.setPassengerId(passengerId); // Set it if not already part of DTO or override
        log.info("Received request to create order: {}", orderCreateDto);
        OrderResponseDto responseDto = orderService.createOrder(orderCreateDto);
        log.info("Order created successfully with ID: {}", responseDto.getOrderId());
        return ApiResponse.success("订单创建请求已提交，等待匹配司机", responseDto);
    }

    @PostMapping("/{orderId}/accept")
    public ApiResponse<String> acceptOrder(@PathVariable Long orderId, @RequestParam Long driverId) {
        // This endpoint might be called by MatchingService internally or an Admin.
        // If called by driver, driverId should come from authenticated principal.
        log.info("Driver {} attempting to accept order {}", driverId, orderId);
        orderService.acceptOrder(orderId, driverId); // This logic will change based on prompt 3
        log.info("Order {} accepted by driver {}", orderId, driverId);
        return ApiResponse.successMessage("订单已接单");
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponseDto> getOrderDetails(@PathVariable Long orderId) {
        log.info("Fetching details for order ID: {}", orderId);
        OrderResponseDto responseDto = orderService.getOrderDetails(orderId);
        return ApiResponse.success(responseDto);
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<String> cancelOrder(@PathVariable Long orderId) {
        // Logic to determine who is cancelling and if allowed (e.g. passengerId from principal)
        // Long cancellingUserId = getAuthenticatedUserId();
        log.info("User attempting to cancel order ID: {}", orderId);
        orderService.cancelOrder(orderId /*, cancellingUserId, userRole */);
        log.info("Order ID: {} cancelled successfully.", orderId);
        return ApiResponse.successMessage("订单已取消");
    }

    @PostMapping("/{orderId}/status")
    public ApiResponse<String> updateOrderStatus(@PathVariable Long orderId, @RequestBody UpdateOrderStatusDto statusDto) {
        // Should be restricted, e.g., only driver can update to ARRIVED, IN_PROGRESS
        // Long principalId = getAuthenticatedUserId();
        log.info("Attempting to update status for order ID: {} to {}", orderId, statusDto.getStatus());
        orderService.updateOrderStatus(orderId, statusDto.getStatus() /*, principalId, principalRole */);
        log.info("Order ID: {} status updated to {}.", orderId, statusDto.getStatus());
        return ApiResponse.successMessage("订单状态已更新");
    }

    // Helper method to get authenticated user ID (conceptual)
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OrderDetailsImpl) {
            OrderDetailsImpl userDetails = (OrderDetailsImpl) authentication.getPrincipal();
            return userDetails.getId();
        }
        throw new IllegalStateException("User not authenticated or details not available.");
    }

}
