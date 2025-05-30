package com.easyride.admin_service.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * 当 order_service 发生异常或特殊情况时，发布 ORDER_EXCEPTION 事件到 "order-topic"。
 * Admin Service 监听后可进行相应处理，比如通知客服或运营人员介入。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OrderExceptionEvent {

    /**
     * 订单唯一标识
     */
    private Long orderId;

    /**
     * 异常类型 / 原因，如 "DRIVER_NO_SHOW", "PAYMENT_FAILURE", "PASSENGER_COMPLAINT" 等
     */
    private String exceptionType;

    /**
     * 详细的异常描述或投诉内容
     */
    private String description;

    /**
     * 触发时间
     */
    private LocalDateTime occurredAt;

    /**
     * 额外信息（可选），例如乘客ID、司机ID
     */
    private Long passengerId;
    private Long driverId;
}
