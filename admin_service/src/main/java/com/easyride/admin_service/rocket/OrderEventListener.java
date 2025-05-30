package com.easyride.admin_service.rocket;

import com.easyride.admin_service.dto.OrderExceptionEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

/**
 * 监听 "order-topic" 中的异常或特殊事件，如订单投诉、异常情况
 */
@Slf4j
@Service
@RocketMQMessageListener(
        topic = "order-topic",
        consumerGroup = "admin-service-group",
        selectorExpression = "ORDER_EXCEPTION" // 如果使用Tag区分
)
public class OrderEventListener implements RocketMQListener<OrderExceptionEvent> {

    @Override
    public void onMessage(OrderExceptionEvent event) {
        // 当 order_service 发布了 ORDER_EXCEPTION 事件时，Admin Service 在这里处理
        log.info("[AdminService] Received order exception event: {}", event);

        // 1. 可以进行相应处理，如记录到后台系统，提醒客服或运营人员
        // 2. 视业务需求可进行数据库存储 / 推送通知
    }
}
