package com.evaluation.controller;

import com.evaluation.dto.AdminReviewActionDto;
import com.evaluation.dto.ApiResponse;
import com.evaluation.dto.ComplaintDTO;
import com.evaluation.dto.EvaluationDTO;
import com.evaluation.service.ComplaintService;
import com.evaluation.service.EvaluationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
// import org.springframework.security.access.prepost.PreAuthorize; // For role-based access

@RestController
@RequestMapping("/admin/reviews") // Base path for admin review operations
// @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')") // Example security
public class AdminReviewController {

        private static final Logger log = LoggerFactory.getLogger(AdminReviewController.class);

        private final EvaluationService evaluationService;
        private final ComplaintService complaintService;

        public AdminReviewController(EvaluationService evaluationService, ComplaintService complaintService) {
                this.evaluationService = evaluationService;
                this.complaintService = complaintService;
        }

        // --- Evaluation Management ---
        @GetMapping("/evaluations")
        public ApiResponse<Page<EvaluationDTO>> getAllEvaluations(Pageable pageable,
                        @RequestParam(required = false) String status) {
                log.info("Admin request to get all evaluations with status filter: {}", status);
                Page<EvaluationDTO> evaluations = evaluationService.getAllEvaluationsForAdmin(pageable, status);
                return ApiResponse.success(evaluations);
        }

        @PutMapping("/evaluations/{evaluationId}/action")
        public ApiResponse<EvaluationDTO> performActionOnEvaluation(
                        @PathVariable Long evaluationId,
                        @Valid @RequestBody AdminReviewActionDto actionDto /*
                                                                            * , @AuthenticationPrincipal UserPrincipal
                                                                            * adminPrincipal
                                                                            */) {
                // Long adminId = adminPrincipal.getId();
                Long adminId = 0L; // Placeholder for admin ID
                log.info("Admin {} performing action on evaluation {}: {}", adminId, evaluationId,
                                actionDto.getNewEvaluationStatus());
                EvaluationDTO updatedEvaluation = evaluationService.adminUpdateEvaluationStatus(evaluationId,
                                actionDto.getNewEvaluationStatus(), actionDto.getAdminNotes(), adminId);
                return ApiResponse.success("评价状态更新成功", updatedEvaluation);
        }

        // --- Complaint Management ---
        @GetMapping("/complaints")
        public ApiResponse<Page<ComplaintDTO>> getAllComplaints(Pageable pageable,
                        @RequestParam(required = false) String status) {
                log.info("Admin request to get all complaints with status filter: {}", status);
                Page<ComplaintDTO> complaints = complaintService.getAllComplaintsForAdmin(pageable, status);
                return ApiResponse.success(complaints);
        }

        @PutMapping("/complaints/{complaintId}/action")
        public ApiResponse<ComplaintDTO> performActionOnComplaint(
                        @PathVariable Long complaintId,
                        @Valid @RequestBody AdminReviewActionDto actionDto /*
                                                                            * , @AuthenticationPrincipal UserPrincipal
                                                                            * adminPrincipal
                                                                            */) {
                // Long adminId = adminPrincipal.getId();
                Long adminId = 0L; // Placeholder
                log.info("Admin {} performing action on complaint {}: {}", adminId, complaintId,
                                actionDto.getNewComplaintStatus());
                ComplaintDTO updatedComplaint = complaintService.adminUpdateComplaintStatus(complaintId,
                                actionDto.getNewComplaintStatus(), actionDto.getAdminNotes(), adminId);
                return ApiResponse.success("投诉状态更新成功", updatedComplaint);
        }
}