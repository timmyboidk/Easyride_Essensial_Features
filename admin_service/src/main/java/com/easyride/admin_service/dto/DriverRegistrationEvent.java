package com.easyride.admin_service.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * 当 user_service 发送司机注册事件时，Admin Service 通过 RocketMQ 监听并处理，
 * 例如自动/人工审核司机信息。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class DriverRegistrationEvent {

    /**
     * 司机的唯一ID（由 user_service 创建时生成）
     */
    private Long driverId;

    /**
     * 司机姓名或用户名
     */
    private String driverName;

    /**
     * 驾照号码或其他证件信息
     */
    private String licenseNumber;

    /**
     * 注册时间（user_service 中创建司机账号的时间）
     */
    private LocalDateTime registrationTime;

    /**
     * 是否需要额外资料审核；可由 user_service 决定
     */
    private boolean needReview;
}
