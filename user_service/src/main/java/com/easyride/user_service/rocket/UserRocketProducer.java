package com.easyride.user_service.rocket;

import com.easyride.user_service.dto.UserEventDto;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

/**
 * 将原来的 Kafka Producer 替换为 RocketMQ Producer
 */
@Service
public class UserRocketProducer {

    private final RocketMQTemplate rocketMQTemplate;

    public UserRocketProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /**
     * 使用 RocketMQTemplate 发送消息
     */
    public void sendUserEvent(UserEventDto userEvent) {
        // "user-topic" 格式示例： "<topic>:<tag>"
        // 其中 tag 可选，tag 用于做消息筛选
        String topicWithTag = "user-topic:USER_CREATED";
        rocketMQTemplate.convertAndSend(topicWithTag, userEvent);
    }
}
