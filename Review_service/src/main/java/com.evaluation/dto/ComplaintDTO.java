package com.evaluation.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 投诉数据传输对象，用于在不同层之间传输投诉数据
 */
@Data
@NoArgsConstructor
public class ComplaintDTO {

    private Long id;
    private Long evaluationId;
    private Long complainantId;
    private String reason;
    private List<String> evidencePaths;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 全参构造函数
     *
     * @param id             投诉ID
     * @param evaluationId   关联的评价ID
     * @param complainantId  申诉者ID
     * @param reason         申诉理由
     * @param evidencePaths  上传的证据路径列表
     * @param status         投诉状态
     * @param createdAt      创建时间
     * @param updatedAt      更新时间
     */
    public ComplaintDTO(Long id, Long evaluationId, Long complainantId, String reason,
                        List<String> evidencePaths, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.evaluationId = evaluationId;
        this.complainantId = complainantId;
        this.reason = reason;
        this.evidencePaths = evidencePaths;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
