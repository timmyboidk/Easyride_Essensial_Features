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

    public EvaluationDTO() {
    }

    /**
     * 全参构造函数
     *
     * @param id              评价ID
     * @param orderId         订单ID
     * @param evaluatorId     评价者ID
     * @param evaluateeId     被评价者ID
     * @param score           评分
     * @param comment         评价内容
     * @param tags            评价标签
     * @param createdAt       创建时间
     * @param updatedAt       更新时间
     * @param complaintStatus 申诉状态
     */
    public EvaluationDTO(Long id, Long orderId, Long evaluatorId, Long evaluateeId, int score, String comment,
            List<String> tags, LocalDateTime createdAt, LocalDateTime updatedAt, String complaintStatus) {
        this.id = id;
        this.orderId = orderId;
        this.evaluatorId = evaluatorId;
        this.evaluateeId = evaluateeId;
        this.score = score;
        this.comment = comment;
        this.tags = tags;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.complaintStatus = complaintStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getEvaluatorId() {
        return evaluatorId;
    }

    public void setEvaluatorId(Long evaluatorId) {
        this.evaluatorId = evaluatorId;
    }

    public Long getEvaluateeId() {
        return evaluateeId;
    }

    public void setEvaluateeId(Long evaluateeId) {
        this.evaluateeId = evaluateeId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getComplaintStatus() {
        return complaintStatus;
    }

    public void setComplaintStatus(String complaintStatus) {
        this.complaintStatus = complaintStatus;
    }
}
