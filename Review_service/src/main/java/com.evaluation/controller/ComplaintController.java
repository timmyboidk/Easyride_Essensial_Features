package com.evaluation.controller;

import com.evaluation.dto.ApiResponse;
import com.evaluation.dto.ComplaintDTO;
import com.evaluation.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    @Autowired
    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /**
     * 提交投诉
     * @param complaintDTO 投诉数据
     * @return 创建的投诉信息
     */
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<ComplaintDTO>> fileComplaint(
            @Valid @RequestPart("complaint") ComplaintDTO complaintDTO,
            @RequestPart(name = "evidenceFiles", required = false) List<MultipartFile> evidenceFiles) {

        ComplaintDTO createdComplaint = complaintService.fileComplaint(complaintDTO, evidenceFiles);

        ApiResponse<ComplaintDTO> response = new ApiResponse<>(
                201,
                "投诉已提交",
                createdComplaint
        );
        return ResponseEntity.status(201).body(response);
    }

    /**
     * 获取与特定评价相关的所有投诉
     * @param evaluationId 评价ID
     * @return 投诉列表
     */
    @GetMapping("/evaluation/{evaluationId}")
    public ResponseEntity<ApiResponse<List<ComplaintDTO>>> getComplaintsByEvaluation(
            @PathVariable Long evaluationId) {
        List<ComplaintDTO> complaints = complaintService.getComplaintsByEvaluation(evaluationId);
        ApiResponse<List<ComplaintDTO>> response = new ApiResponse<>(
                200,
                "查询成功",
                complaints
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 获取特定申诉者的所有投诉
     * @param complainantId 申诉者ID
     * @return 投诉列表
     */
    @GetMapping("/complainant/{complainantId}")
    public ResponseEntity<ApiResponse<List<ComplaintDTO>>> getComplaintsByComplainant(
            @PathVariable Long complainantId) {
        List<ComplaintDTO> complaints = complaintService.getComplaintsByComplainant(complainantId);
        ApiResponse<List<ComplaintDTO>> response = new ApiResponse<>(
                200,
                "查询成功",
                complaints
        );
        return ResponseEntity.ok(response);
    }
}
