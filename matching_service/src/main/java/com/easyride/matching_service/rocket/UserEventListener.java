package com.easyride.matching_service.rocket;

import com.easyride.matching_service.dto.UserEventDto;
import com.easyride.matching_service.model.DriverStatus;
import com.easyride.matching_service.repository.DriverStatusMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RocketMQMessageListener(topic = "EASYRIDE_USER_TOPIC", consumerGroup = "CID_MATCHING_SERVICE", selectorExpression = "USER_CREATED || DRIVER_APPROVED || USER_UPDATED")
public class UserEventListener implements RocketMQListener<UserEventDto> {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    @Autowired
    private DriverStatusMapper driverStatusMapper;

    @Override
    public void onMessage(UserEventDto event) {
        log.info("Received UserEvent: {}", event);
        if (!"DRIVER".equalsIgnoreCase(event.getRole())) {
            log.debug("Ignoring user event for non-driver role: {}", event.getRole());
            return;
        }

        try {
            if ("USER_CREATED".equals(event.getEventType()) || "DRIVER_APPROVED".equals(event.getEventType())) {
                DriverStatus existingStatus = driverStatusMapper.selectById(event.getUserId());

                final DriverStatus status;
                if (existingStatus == null) {
                    // New driver - create fresh status
                    status = new DriverStatus();
                    status.setDriverId(event.getUserId());
                    status.setAvailable(true);
                    status.setRating(event.getInitialRating() != null ? event.getInitialRating() : 4.5);
                    status.setVehicleType(event.getVehicleType() != null ? event.getVehicleType() : "UNKNOWN");
                    status.setLastStatusUpdateTime(LocalDateTime.now());
                    if ("DRIVER_APPROVED".equals(event.getEventType())) {
                        status.setOnlineSince(LocalDateTime.now());
                    }
                    driverStatusMapper.insert(status);
                } else {
                    // Existing driver - update status
                    existingStatus.setAvailable(true);
                    existingStatus.setRating(
                            event.getInitialRating() != null ? event.getInitialRating() : existingStatus.getRating());
                    if (event.getVehicleType() != null) {
                        existingStatus.setVehicleType(event.getVehicleType());
                    }
                    existingStatus.setLastStatusUpdateTime(LocalDateTime.now());
                    if (existingStatus.getOnlineSince() == null && "DRIVER_APPROVED".equals(event.getEventType())) {
                        existingStatus.setOnlineSince(LocalDateTime.now());
                    }
                    driverStatusMapper.updateById(existingStatus);
                }
                log.info("Upserted DriverStatus for driver ID {} due to event {}", event.getUserId(),
                        event.getEventType());

            } else if ("USER_UPDATED".equals(event.getEventType())) {
                // Handle updates to driver profile relevant to matching (e.g., vehicle type
                // change)
                DriverStatus driverStatus = driverStatusMapper.selectById(event.getUserId());
                if (driverStatus != null) {
                    boolean changed = false;
                    if (event.getVehicleType() != null
                            && !event.getVehicleType().equals(driverStatus.getVehicleType())) {
                        driverStatus.setVehicleType(event.getVehicleType());
                        changed = true;
                    }
                    // Add other updatable fields like rating if User Service sends them
                    if (changed) {
                        driverStatus.setLastStatusUpdateTime(LocalDateTime.now());
                        driverStatusMapper.updateById(driverStatus);
                        log.info("Updated DriverStatus for driver ID {} from USER_UPDATED event", event.getUserId());
                    }
                }
            }
            // Handle USER_DISABLED or DRIVER_SUSPENDED events to mark driver as unavailable
        } catch (

        Exception e) {
            log.error("Error processing UserEvent for driverId {}: ", event.getUserId(), e);
            // Consider DLQ
        }
    }
    // private int determineCapacityFromVehicleType(String vehicleType) { ... }
}