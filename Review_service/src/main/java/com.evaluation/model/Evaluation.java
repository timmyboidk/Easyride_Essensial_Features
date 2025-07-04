package com.evaluation.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评价实体类
 */
@Entity
@Table(name = "evaluations")
@Data
@NoArgsConstructor
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 评价者ID
    private Long evaluatorId;

    // 被评价者ID
    private Long evaluateeId;

    // 评分
    private int score;

    // 评价内容
    private String comment;

    @Enumerated(EnumType.STRING)
    private EvaluationStatus status = EvaluationStatus.ACTIVE; // Default to active

    private String adminNotes; // Notes from admin after review
    private Long reviewedByAdminId;
    private LocalDateTime reviewTime;


    // 评价标签
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "evaluation_tags",
            joinColumns = @JoinColumn(name = "evaluation_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags;

    // 申诉状态
    private String complaintStatus;

    // 创建时间
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.complaintStatus == null) {
            this.complaintStatus = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
