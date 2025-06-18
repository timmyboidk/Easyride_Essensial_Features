package com.easyride.order_service.dto;

import com.easyride.order_service.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventDto {
    private Long orderId;
    private Long passengerId;
    private Long driverId;
    private OrderStatus status;
    private String message;
    private LocalDateTime timestamp;
}