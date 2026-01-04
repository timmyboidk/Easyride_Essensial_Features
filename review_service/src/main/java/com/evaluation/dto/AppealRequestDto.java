package com.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class AppealRequestDto {
    private Long complaintIdToAppeal; // ID of the complaint being appealed
    private Long evaluationIdToAppeal; // ID of the evaluation being appealed (can be one or the other)

    @NotNull(message = "申诉人ID不能为空") // Should be taken from authenticated principal
    private Long appellantId;

    @NotBlank(message = "申诉理由不能为空")
    private String appealReason;

    private List<String> evidenceLinks; // Optional links to supporting evidence
}