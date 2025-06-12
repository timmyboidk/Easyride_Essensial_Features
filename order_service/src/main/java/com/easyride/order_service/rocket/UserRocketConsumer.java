package com.easyride.order_service.rocket;

import com.easyride.order_service.dto.UserEventDto;
import com.easyride.order_service.model.*;
import com.easyride.order_service.repository.DriverRepository;
import com.easyride.order_service.repository.PassengerRepository;
import com.easyride.order_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = "${rocketmq.consumer.user-topic}",
        consumerGroup = "${rocketmq.consumer.user-group}"
)
public class UserRocketConsumer implements RocketMQListener<UserEventDto> {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private DriverRepository driverRepository;


    @Override
    public void onMessage(UserEventDto userEvent) {
        log.info("Received user event: {}", userEvent);
        try {
            User user = userRepository.findById(userEvent.getUserId()).orElse(null);
            if (user == null) {
                // Create new user based on role
                if (userEvent.getRole() == Role.PASSENGER) {
                    Passenger passenger = new Passenger();
                    updateUserData(passenger, userEvent);
                    passengerRepository.save(passenger);
                } else if (userEvent.getRole() == Role.DRIVER) {
                    Driver driver = new Driver();
                    updateUserData(driver, userEvent);
                    driverRepository.save(driver);
                }
            } else {
                // Update existing user
                updateUserData(user, userEvent);
                userRepository.save(user);
            }
        } catch (Exception e) {
            log.error("Error processing user event for userId: {}", userEvent.getUserId(), e);
        }
    }

    private void updateUserData(User user, UserEventDto userEvent) {
        user.setId(userEvent.getUserId());
        user.setUsername(userEvent.getUsername());
        user.setEmail(userEvent.getEmail());
        user.setPhoneNumber(userEvent.getPhoneNumber());
        user.setRole(userEvent.getRole());
    }
}
