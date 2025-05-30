package com.easyride.admin_service.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * 当管理员在后台手动干预订单时发送的事件
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class AdminOrderInterveneEvent {
    private Long orderId;
    private String action;        // "REASSIGN", "CANCEL" 等
    private String reason;        // 干预原因
    private Long adminUserId;     // 哪个管理员执行的操作
    private LocalDateTime operateTime;
}
