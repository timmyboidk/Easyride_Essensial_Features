package com.easyride.matching_service.rocket;

import com.easyride.matching_service.dto.UserEventDto;
import com.easyride.matching_service.model.DriverStatus;
import com.easyride.matching_service.repository.DriverStatusRepository;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RocketMQMessageListener(
        topic = "user-topic", // Topic from User Service
        consumerGroup = "matching-service-user-consumer-group",
        selectorExpression = "USER_CREATED || DRIVER_APPROVED || USER_UPDATED" // Listen to relevant events
)
public class UserEventListener implements RocketMQListener<UserEventDto> {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    @Autowired
    private DriverStatusRepository driverStatusRepository;

    @Override
    public void onMessage(UserEventDto event) {
        log.info("Received UserEvent: {}", event);
        if (!"DRIVER".equalsIgnoreCase(event.getRole())) {
            log.debug("Ignoring user event for non-driver role: {}", event.getRole());
            return;
        }

        try {
            if ("USER_CREATED".equals(event.getEventType()) || "DRIVER_APPROVED".equals(event.getEventType())) {
                DriverStatus status = driverStatusRepository.findById(event.getUserId())
                        .orElse(new DriverStatus());

                status.setDriverId(event.getUserId());
                status.setAvailable(true); // Default to available when approved/created
                status.setRating(event.getInitialRating() != null ? event.getInitialRating() : 4.5); // Default new driver rating
                status.setVehicleType(event.getVehicleType() != null ? event.getVehicleType() : "UNKNOWN"); // Get from event
                // status.setMaxCapacityForCarpool(determineCapacityFromVehicleType(event.getVehicleType()));
                status.setLastStatusUpdateTime(LocalDateTime.now());
                if (status.getOnlineSince() == null && "DRIVER_APPROVED".equals(event.getEventType())) {
                    status.setOnlineSince(LocalDateTime.now()); // Or when they first toggle online
                }
                driverStatusRepository.save(status);
                log.info("Upserted DriverStatus for driver ID {} due to event {}", event.getUserId(), event.getEventType());

            } else if ("USER_UPDATED".equals(event.getEventType())) {
                // Handle updates to driver profile relevant to matching (e.g., vehicle type change)
                driverStatusRepository.findById(event.getUserId()).ifPresent(driverStatus -> {
                    boolean changed = false;
                    if(event.getVehicleType() != null && !event.getVehicleType().equals(driverStatus.getVehicleType())) {
                        driverStatus.setVehicleType(event.getVehicleType());
                        changed = true;
                    }
                    // Add other updatable fields like rating if User Service sends them
                    if(changed) {
                        driverStatus.setLastStatusUpdateTime(LocalDateTime.now());
                        driverStatusRepository.save(driverStatus);
                        log.info("Updated DriverStatus for driver ID {} from USER_UPDATED event", event.getUserId());
                    }
                });
            }
            // Handle USER_DISABLED or DRIVER_SUSPENDED events to mark driver as unavailable
        } catch (Exception e) {
            log.error("Error processing UserEvent for driverId {}: ", event.getUserId(), e);
            // Consider DLQ
        }
    }
    // private int determineCapacityFromVehicleType(String vehicleType) { ... }
}