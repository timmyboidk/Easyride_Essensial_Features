package com.easyride.admin_service.rocket;

// Existing DTO: com.easyride.admin_service.dto.DriverRegistrationEvent
// Better to use a more specific DTO from User Service if available
// e.g., com.easyride.user_service.dto.DriverApplicationEventDto
import com.easyride.admin_service.dto.DriverApplicationEventDto_Consumed; // New DTO mirroring what User Service sends
import com.easyride.admin_service.service.AdminDriverManagementService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
        topic = "user-topic", // As defined by User Service
        consumerGroup = "${rocketmq.consumer.group}",
        selectorExpression = "DRIVER_APPLICATION_SUBMITTED" // Matches tag from UserRocketProducer
)
public class DriverRegListener implements RocketMQListener<DriverApplicationEventDto_Consumed> {

    private static final Logger log = LoggerFactory.getLogger(DriverRegListener.class);

    @Autowired
    private AdminDriverManagementService driverManagementService;

    @Override
    public void onMessage(DriverApplicationEventDto_Consumed event) {
        log.info("Received DRIVER_APPLICATION_SUBMITTED event: DriverId={}, Username={}", event.getDriverId(), event.getUsername());
        try {
            driverManagementService.processNewDriverApplication(event);
        } catch (Exception e) {
            log.error("Error processing driver registration event for driverId {}: ", event.getDriverId(), e);
            // Implement retry or DLQ logic
        }
    }
}