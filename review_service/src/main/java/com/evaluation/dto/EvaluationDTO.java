package com.evaluation.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评价数据传输对象，用于在不同层之间传输评价数据
 */
@Data
@NoArgsConstructor
public class EvaluationDTO {

    private Long id;
    private Long orderId;
    private Long evaluatorId;
    private Long evaluateeId;
    private int score;
    private String comment;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String complaintStatus;
}
