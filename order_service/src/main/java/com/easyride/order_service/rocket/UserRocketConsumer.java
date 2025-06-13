package com.easyride.order_service.rocket;

import com.easyride.order_service.dto.UserEventDto;
import com.easyride.order_service.model.Driver;
import com.easyride.order_service.model.Passenger;
import com.easyride.order_service.model.Role;
import com.easyride.order_service.model.User;
import com.easyride.order_service.repository.DriverRepository;
import com.easyride.order_service.repository.PassengerRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = "${rocketmq.consumer.user-topic}",
        consumerGroup = "${rocketmq.consumer.user-group}"
)
public class UserRocketConsumer implements RocketMQListener<UserEventDto> {

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Override
    @Transactional
    public void onMessage(UserEventDto userEvent) {
        if (userEvent.getRole() == null) {
            log.error("Received user event with null role. Event: {}", userEvent);
            return;
        }

        log.info("Received user event for user ID: {} and role: {}", userEvent.getId(), userEvent.getRole());
        try {
            Role userRole = Role.valueOf(userEvent.getRole().toUpperCase());

            if (userRole == Role.PASSENGER) {
                Passenger passenger = passengerRepository.findById(userEvent.getId())
                        .orElseGet(() -> {
                            log.info("Creating new local record for passenger with ID: {}", userEvent.getId());
                            return new Passenger();
                        });
                updateUserData(passenger, userEvent);
                passengerRepository.save(passenger);
                log.info("Successfully processed passenger event for ID: {}", userEvent.getId());

            } else if (userRole == Role.DRIVER) {
                Driver driver = driverRepository.findById(userEvent.getId())
                        .orElseGet(() -> {
                            log.info("Creating new local record for driver with ID: {}", userEvent.getId());
                            return new Driver();
                        });
                updateUserData(driver, userEvent);
                driverRepository.save(driver);
                log.info("Successfully processed driver event for ID: {}", userEvent.getId());
            } else {
                log.warn("Received user event with unhandled role: {}", userEvent.getRole());
            }
        } catch (Exception e) {
            log.error("Error processing user event for userId: {}. Error: {}", userEvent.getId(), e.getMessage(), e);
        }
    }

    /**
     * Helper method to update common user fields from the event DTO.
     * @param user The user entity (Passenger or Driver) to update.
     * @param userEvent The DTO containing the new data.
     */
    private void updateUserData(User user, UserEventDto userEvent) {
        user.setId(userEvent.getId()); // Correctly uses getId()
        user.setUsername(userEvent.getUsername());
        user.setEmail(userEvent.getEmail());
        user.setRole(Role.valueOf(userEvent.getRole().toUpperCase())); // Correctly converts String to Enum
        // Phone number is not set because it is not present in the provided UserEventDto
    }
}