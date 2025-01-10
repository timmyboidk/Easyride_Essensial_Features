package com.evaluation.mapper;

import com.evaluation.dto.ComplaintDTO;
import com.evaluation.model.Complaint;
import org.springframework.stereotype.Component;

/**
 * 投诉映射器，用于实体和DTO之间的转换
 */
@Component
public class ComplaintMapper {

    /**
     * 将 Complaint 实体转换为 ComplaintDTO
     *
     * @param complaint 投诉实体
     * @return 投诉DTO
     */
    public ComplaintDTO toDTO(Complaint complaint) {
        ComplaintDTO dto = new ComplaintDTO();
        dto.setId(complaint.getId());
        dto.setEvaluationId(complaint.getEvaluationId());
        dto.setComplainantId(complaint.getComplainantId());
        dto.setReason(complaint.getReason());
        dto.setEvidencePaths(complaint.getEvidencePaths());
        dto.setStatus(complaint.getStatus());
        dto.setCreatedAt(complaint.getCreatedAt());
        dto.setUpdatedAt(complaint.getUpdatedAt());
        return dto;
    }

    /**
     * 将 ComplaintDTO 转换为 Complaint 实体
     *
     * @param dto 投诉DTO
     * @return 投诉实体
     */
    public Complaint toEntity(ComplaintDTO dto) {
        Complaint complaint = new Complaint();
        complaint.setEvaluationId(dto.getEvaluationId());
        complaint.setComplainantId(dto.getComplainantId());
        complaint.setReason(dto.getReason());
        complaint.setEvidencePaths(dto.getEvidencePaths());
        complaint.setStatus(dto.getStatus());
        return complaint;
    }
}

