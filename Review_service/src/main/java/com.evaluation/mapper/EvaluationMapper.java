package com.evaluation.mapper;

import com.evaluation.dto.EvaluationDTO;
import com.evaluation.model.Evaluation;
import com.evaluation.model.Tag;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评价映射器，用于实体和DTO之间的转换
 */
@Component
public class EvaluationMapper {

    /**
     * 将 Evaluation 实体转换为 EvaluationDTO
     *
     * @param evaluation 评价实体
     * @return 评价DTO
     */
    public EvaluationDTO toDTO(Evaluation evaluation) {
        EvaluationDTO dto = new EvaluationDTO();
        dto.setId(evaluation.getId());
        dto.setEvaluatorId(evaluation.getEvaluatorId());
        dto.setEvaluateeId(evaluation.getEvaluateeId());
        dto.setScore(evaluation.getScore());
        dto.setComment(evaluation.getComment());
        dto.setComplaintStatus(evaluation.getComplaintStatus());
        dto.setCreatedAt(evaluation.getCreatedAt());
        dto.setUpdatedAt(evaluation.getUpdatedAt());

        if (evaluation.getTags() != null && !evaluation.getTags().isEmpty()) {
            Set<String> tagNames = evaluation.getTags().stream()
                    .map(Tag::getName)
                    .collect(Collectors.toSet());
            dto.setTags(tagNames);
        }

        return dto;
    }

    /**
     * 将 EvaluationDTO 转换为 Evaluation 实体
     *
     * @param dto 评价DTO
     * @return 评价实体
     */
    public Evaluation toEntity(EvaluationDTO dto) {
        Evaluation evaluation = new Evaluation();
        evaluation.setEvaluatorId(dto.getEvaluatorId());
        evaluation.setEvaluateeId(dto.getEvaluateeId());
        evaluation.setScore(dto.getScore());
        evaluation.setComment(dto.getComment());
        evaluation.setComplaintStatus(dto.getComplaintStatus());
        // 标签处理在 Service 层完成
        return evaluation;
    }
}
