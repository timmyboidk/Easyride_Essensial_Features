package com.evaluation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "appeals")
@Data
@NoArgsConstructor
public class Appeal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Can appeal a Complaint or a specific Evaluation
    @OneToOne
    @JoinColumn(name = "complaint_id", unique = true) // An appeal is for one complaint
    private Complaint complaint;

    @ManyToOne // Or OneToOne if an appeal is strictly for one evaluation
    @JoinColumn(name = "evaluation_id")
    private Evaluation evaluation; // If appealing an evaluation directly

    @Column(nullable = false)
    private Long appellantId; // User ID of the person appealing

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String appealReason;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String evidenceLinks; // JSON array of links or paths to evidence

    @Enumerated(EnumType.STRING)
    private AppealStatus status; // PENDING_REVIEW, UNDER_INVESTIGATION, UPHELD, REJECTED

    private LocalDateTime appealTime;
    private LocalDateTime lastUpdated;

    private String adminNotesOnAppeal;
    private Long reviewedByAdminId;
    private LocalDateTime appealReviewTime;

    @PrePersist
    protected void onCreate() {
        this.appealTime = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.status = AppealStatus.PENDING_REVIEW;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
}