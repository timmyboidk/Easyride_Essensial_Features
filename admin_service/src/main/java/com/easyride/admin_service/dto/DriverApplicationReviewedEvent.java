package com.easyride.admin_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * 司机申请审核完成事件
 * 当管理员批准或拒绝申请时，此事件被发送到消息队列。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverApplicationReviewedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long driverId;
    private String finalStatus; // "APPROVED" 或 "REJECTED"
    private String reviewNotes;
}