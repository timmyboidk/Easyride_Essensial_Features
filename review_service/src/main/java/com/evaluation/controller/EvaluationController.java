package com.evaluation.controller;

import com.evaluation.dto.ApiResponse;
import com.evaluation.dto.EvaluationDTO;
import com.evaluation.service.EvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /**
     * 创建新的评价
     * 
     * @param evaluationDTO 评价数据
     * @return 创建的评价信息
     */
    @PostMapping
    public ResponseEntity<ApiResponse<EvaluationDTO>> createEvaluation(
            @Valid @RequestBody EvaluationDTO evaluationDTO) {
        EvaluationDTO createdEvaluation = evaluationService.createEvaluation(evaluationDTO);

        // Sanitize for XSS prevention
        sanitizeEvaluation(createdEvaluation);

        ApiResponse<EvaluationDTO> response = new ApiResponse<>(
                201,
                "评价已创建",
                createdEvaluation);
        return ResponseEntity.status(201).body(response);
    }

    private void sanitizeEvaluation(EvaluationDTO dto) {
        if (dto == null)
            return;
        if (dto.getComment() != null) {
            dto.setComment(org.springframework.web.util.HtmlUtils.htmlEscape(dto.getComment()));
        }
        if (dto.getTags() != null) {
            List<String> sanitizedTags = dto.getTags().stream()
                    .map(tag -> tag == null ? null : org.springframework.web.util.HtmlUtils.htmlEscape(tag))
                    .toList();
            dto.setTags(sanitizedTags);
        }
        if (dto.getStatus() != null) {
            dto.setStatus(org.springframework.web.util.HtmlUtils.htmlEscape(dto.getStatus()));
        }
    }

    /**
     * 获取被评价者的所有评价
     * 
     * @param evaluateeId 被评价者ID
     * @return 评价列表
     */
    @GetMapping("/evaluatee/{evaluateeId}")
    public ResponseEntity<ApiResponse<List<EvaluationDTO>>> getEvaluationsByEvaluatee(
            @PathVariable Long evaluateeId) {
        List<EvaluationDTO> evaluations = evaluationService.getEvaluationsByEvaluatee(evaluateeId);
        ApiResponse<List<EvaluationDTO>> response = new ApiResponse<>(
                200,
                "查询成功",
                evaluations);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取评价者的所有评价
     * 
     * @param evaluatorId 评价者ID
     * @return 评价列表
     */
    @GetMapping("/evaluator/{evaluatorId}")
    public ResponseEntity<ApiResponse<List<EvaluationDTO>>> getEvaluationsByEvaluator(
            @PathVariable Long evaluatorId) {
        List<EvaluationDTO> evaluations = evaluationService.getEvaluationsByEvaluator(evaluatorId);
        ApiResponse<List<EvaluationDTO>> response = new ApiResponse<>(
                200,
                "查询成功",
                evaluations);
        return ResponseEntity.ok(response);
    }
}
