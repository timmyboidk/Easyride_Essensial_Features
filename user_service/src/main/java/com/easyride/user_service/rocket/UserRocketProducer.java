package com.easyride.user_service.rocket;

import com.easyride.user_service.dto.UserEventDto;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import com.easyride.user_service.dto.DriverApplicationEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        // Topic from MQ.md: EASYRIDE_USER_REGISTERED_TOPIC
        String topic = "EASYRIDE_USER_REGISTERED_TOPIC";
        rocketMQTemplate.convertAndSend(topic, userEvent);
        log.info("Sent user registered event to topic {}: {}", topic, userEvent);
    }

    public void sendDriverApplicationEvent(DriverApplicationEventDto event) {
        // Topic from MQ.md: EASYRIDE_DRIVER_APPLICATION_SUBMITTED_TOPIC
        String topic = "EASYRIDE_DRIVER_APPLICATION_SUBMITTED_TOPIC";
        rocketMQTemplate.convertAndSend(topic, event);
        log.info("Sent driver application submitted event to topic {}: {}", topic, event);
    }
}
