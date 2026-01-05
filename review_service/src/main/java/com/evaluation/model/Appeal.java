package com.evaluation.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("appeals")
@Data
@NoArgsConstructor
public class Appeal {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long complaintId;
    private Long evaluationId;
    private Long appellantId;
    private String appealReason;
    private String evidenceLinks;
    private AppealStatus status;
    private LocalDateTime appealTime;
    private LocalDateTime lastUpdated;
    private String adminNotesOnAppeal;
    private Long reviewedByAdminId;
    private LocalDateTime appealReviewTime;
}