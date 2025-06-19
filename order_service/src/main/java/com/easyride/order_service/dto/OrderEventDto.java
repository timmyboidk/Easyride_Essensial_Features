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
    private double startLatitude;
    private double startLongitude;

    public OrderEventDto(Long orderId, Long passengerId, Long driverId, String status, LocalDateTime timestamp, String message, double startLatitude, double startLongitude) {
        this.orderId = orderId;
        this.passengerId = passengerId;
        this.driverId = driverId;
        this.status = OrderStatus.valueOf(status);
        this.timestamp = timestamp;
        this.message = message;
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
    }
}