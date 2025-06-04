package com.easyride.user_service.rocket;

import com.easyride.user_service.dto.UserEventDto;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import com.easyride.user_service.dto.DriverApplicationEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 将原来的 Kafka Producer 替换为 RocketMQ Producer
 */
@Service
public class UserRocketProducer {

    private static final Logger log = LoggerFactory.getLogger(UserRocketProducer.class);
    private final RocketMQTemplate rocketMQTemplate;

    public UserRocketProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public void sendUserEvent(UserEventDto userEvent) {
        // Using eventType from DTO as part of the tag for more specific consumption
        String topicWithTag = "user-topic:" + userEvent.getEventType();
        rocketMQTemplate.convertAndSend(topicWithTag, userEvent);
        log.info("Sent user event to topic {}: {}", topicWithTag, userEvent);
    }

    public void sendDriverApplicationEvent(DriverApplicationEventDto event) {
        // Specific topic or tag for driver applications to be consumed by Admin Service
        String topicWithTag = "user-topic:DRIVER_APPLICATION_SUBMITTED";
        rocketMQTemplate.convertAndSend(topicWithTag, event);
        log.info("Sent driver application event to topic {}: {}", topicWithTag, event);
    }
}
