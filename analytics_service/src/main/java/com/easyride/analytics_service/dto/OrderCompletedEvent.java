package com.easyride.analytics_service.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 与 order_service 发送的事件格式对应
 * 假设 order_service 在某处使用 rocketMQTemplate.convertAndSend("order-topic", new OrderCompletedEvent(...))
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderCompletedEvent {
    private Long orderId;
    private Long passengerId;
    private Double orderAmount;
    private String region;
    private LocalDateTime completedTime;
    private double finalAmount;
    private LocalDateTime orderCompletionTime;
    private Long driverId;
    private String serviceType;
    private String vehicleType;
}
