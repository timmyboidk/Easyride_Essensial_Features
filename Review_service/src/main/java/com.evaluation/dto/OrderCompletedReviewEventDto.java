package com.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedReviewEventDto { // Consumed from Order Service (e.g. order-topic:ORDER_PAYMENT_SETTLED)
    private Long orderId;
    private Long passengerId;
    private Long driverId;
    private LocalDateTime tripEndTime;
    // Any other data useful for initiating review process
}