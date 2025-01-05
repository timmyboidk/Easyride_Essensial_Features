package com.easyride.admin_service.rocket;

import com.easyride.admin_service.dto.AdminOrderInterveneEvent;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

/**
 * Admin Service 的 RocketMQ 发送者，用于后台干预订单等操作
 */
@Service
public class AdminRocketProducer {

    private final RocketMQTemplate rocketMQTemplate;

    public AdminRocketProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /**
     * 将后台干预订单事件发送到指定topic
     * @param event 干预事件对象
     */
    public void sendOrderInterveneEvent(AdminOrderInterveneEvent event) {
        // 可根据 Tag 区分干预类型，或者在 event 内包含 action
        rocketMQTemplate.convertAndSend("admin-intervene-topic", event);
    }
}
