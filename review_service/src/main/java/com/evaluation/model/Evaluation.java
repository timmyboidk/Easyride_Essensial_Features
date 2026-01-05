package com.evaluation.model;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评价实体类
 */
@TableName("evaluations")
@Data
@NoArgsConstructor
public class Evaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 评价者ID
    private Long evaluatorId;

    // 被评价者ID
    private Long evaluateeId;

    // 评分
    private int score;

    // 评价内容
    private String comment;

    private EvaluationStatus status = EvaluationStatus.ACTIVE; // Default to active

    private String adminNotes; // Notes from admin after review
    private Long reviewedByAdminId;
    private LocalDateTime reviewTime;

    // 申诉状态
    private String complaintStatus;

    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;

    private String tagsString;

    public LocalDateTime getLastUpdated() {
        return updatedAt;
    }

    public void setLastUpdated(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
