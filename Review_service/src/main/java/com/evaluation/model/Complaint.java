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

    public Complaint() {
    }

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(Long evaluationId) {
        this.evaluationId = evaluationId;
    }

    public Long getComplainantId() {
        return complainantId;
    }

    public void setComplainantId(Long complainantId) {
        this.complainantId = complainantId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public Long getHandledByAdminId() {
        return handledByAdminId;
    }

    public void setHandledByAdminId(Long handledByAdminId) {
        this.handledByAdminId = handledByAdminId;
    }

    public LocalDateTime getResolutionTime() {
        return resolutionTime;
    }

    public void setResolutionTime(LocalDateTime resolutionTime) {
        this.resolutionTime = resolutionTime;
    }

    public Appeal getAppeal() {
        return appeal;
    }

    public void setAppeal(Appeal appeal) {
        this.appeal = appeal;
    }

    public List<String> getEvidencePaths() {
        return evidencePaths;
    }

    public void setEvidencePaths(List<String> evidencePaths) {
        this.evidencePaths = evidencePaths;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
