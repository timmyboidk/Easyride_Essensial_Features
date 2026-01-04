package com.easyride.notification_service.rocketmq;

import com.easyride.notification_service.dto.UserRegisteredEvent;
import com.easyride.notification_service.service.NotificationDispatcherService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = "EASYRIDE_USER_REGISTERED_TOPIC", consumerGroup = "${rocketmq.consumer.group}")
public class UserEventListener implements RocketMQListener<UserRegisteredEvent> {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    private final NotificationDispatcherService dispatcherService;

    public UserEventListener(NotificationDispatcherService dispatcherService) {
        this.dispatcherService = dispatcherService;
    }

    @Override
    public void onMessage(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent: UserID={}", event.getUserId());
        try {
            dispatcherService.dispatchUserRegistered(event);
        } catch (Exception e) {
            log.error("Error processing UserRegisteredEvent: ", e);
        }
    }
}
