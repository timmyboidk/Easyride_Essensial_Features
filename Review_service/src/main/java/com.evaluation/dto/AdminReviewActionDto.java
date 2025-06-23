package com.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminReviewActionDto {
    // For Evaluation actions
    private String newEvaluationStatus; // e.g., "ACTIVE", "HIDDEN_BY_ADMIN"

    // For Complaint actions
    private String newComplaintStatus; // e.g., "INVESTIGATING", "RESOLVED", "REJECTED"

    @NotBlank(message = "管理员备注不能为空")
    private String adminNotes;

    // Admin ID will come from authenticated principal
}