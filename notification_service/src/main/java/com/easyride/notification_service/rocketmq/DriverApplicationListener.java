package com.easyride.notification_service.rocketmq;

import com.easyride.notification_service.dto.DriverApplicationEvent;
import com.easyride.notification_service.service.NotificationDispatcherService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = "EASYRIDE_DRIVER_APPLICATION_SUBMITTED_TOPIC", consumerGroup = "${rocketmq.consumer.group}")
public class DriverApplicationListener implements RocketMQListener<DriverApplicationEvent> {

    private static final Logger log = LoggerFactory.getLogger(DriverApplicationListener.class);

    @Autowired
    private NotificationDispatcherService dispatcherService;

    @Override
    public void onMessage(DriverApplicationEvent event) {
        log.info("Received DriverApplicationEvent: DriverUserID={}", event.getDriverUserId());
        try {
            dispatcherService.dispatchDriverApplication(event);
        } catch (Exception e) {
            log.error("Error processing DriverApplicationEvent: ", e);
        }
    }
}
