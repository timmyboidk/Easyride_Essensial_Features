package com.evaluation.dto;

import java.time.LocalDateTime;
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
    private Long evaluatorId;
    private Long evaluateeId;
    private int score;
    private String comment;
    private Set<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String complaintStatus;

    /**
     * 全参构造函数
     *
     * @param id               评价ID
     * @param evaluatorId      评价者ID
     * @param evaluateeId      被评价者ID
     * @param score            评分
     * @param comment          评价内容
     * @param tags             评价标签
     * @param createdAt        创建时间
     * @param updatedAt        更新时间
     * @param complaintStatus  申诉状态
     */
    public EvaluationDTO(Long id, Long evaluatorId, Long evaluateeId, int score, String comment,
                         Set<String> tags, LocalDateTime createdAt, LocalDateTime updatedAt, String complaintStatus) {
        this.id = id;
        this.evaluatorId = evaluatorId;
        this.evaluateeId = evaluateeId;
        this.score = score;
        this.comment = comment;
        this.tags = tags;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.complaintStatus = complaintStatus;
    }
}
