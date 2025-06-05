package com.easyride.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEventDto {
    private Long orderId;
    private Long passengerId;
    private Double amount;
    private String currency;
    private String failureReason;
    private LocalDateTime timestamp;
}
