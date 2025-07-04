package com.evaluation.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 投诉实体类
 */
@Entity
@Table(name = "complaints")
@Data
@NoArgsConstructor
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联的评价ID
    private Long evaluationId;

    // 申诉者ID
    private Long complainantId;

    // 申诉理由
    private String reason;

    private String adminNotes;
    private Long handledByAdminId;
    private LocalDateTime resolutionTime;

    // For appeal
    @OneToOne(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    private Appeal appeal;

    // 上传的证据路径
    @ElementCollection
    @CollectionTable(name = "complaint_evidence", joinColumns = @JoinColumn(name = "complaint_id"))
    @Column(name = "evidence_path")
    private List<String> evidencePaths;

    // 申诉状态（如：PENDING, REVIEWED, RESOLVED, REJECTED）
    private String status;

    // 创建时间
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
