package com.easyride.user_service.dto;

import com.easyride.user_service.model.DriverApprovalStatus;

// 使用 Lombok 简化代码
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 用于管理员更新司机状态和备注的数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverProfileUpdateDto {

    // 审核状态 (e.g., APPROVED, REJECTED)
    private DriverApprovalStatus verificationStatus;

    // 审核备注
    private String reviewNotes;
}