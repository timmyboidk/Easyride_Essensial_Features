package com.evaluation.model;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 投诉实体类
 */
@TableName("complaints")
@Data
@NoArgsConstructor
public class Complaint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long evaluationId;
    private Long complainantId;
    private String reason;
    private String adminNotes;
    private Long handledByAdminId;
    private LocalDateTime resolutionTime;
    private String status;
    private String evidencePathsString;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
