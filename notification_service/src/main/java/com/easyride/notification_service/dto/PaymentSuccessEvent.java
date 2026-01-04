package com.easyride.notification_service.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentSuccessEvent implements Serializable {
    private Long paymentId;
    private Long orderId;
    private Long userId; // The payer
    private BigDecimal amount;
    private LocalDateTime paymentTime;
}
