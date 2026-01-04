package com.evaluation.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 投诉数据传输对象，用于在不同层之间传输投诉数据
 */
@Data
@NoArgsConstructor
public class ComplaintDTO {

    private Long id;
    private Long evaluationId;
    private Long complainantId;
    private String reason;
    private List<String> evidencePaths;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
