package com.easyride.analytics_service.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConsumedReviewEventDto {
    private Long orderId;
    private Long evaluatorId;
    private Long evaluateeId;
    private String evaluatorRole;
    private int score;
    private String eventType;
    private LocalDateTime timestamp;
}