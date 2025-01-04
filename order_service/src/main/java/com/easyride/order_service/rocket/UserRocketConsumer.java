package com.easyride.order_service.rocket;

import com.easyride.order_service.dto.UserEventDto;
import com.easyride.order_service.model.User;
import com.easyride.order_service.repository.UserRepository;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 替代原来的 Kafka Consumer；监听 "user-topic" 主题
 */
@Service
@RocketMQMessageListener(topic = "user-topic", consumerGroup = "order-service-group")
public class UserRocketConsumer {

    private final UserRepository userRepository;

    public UserRocketConsumer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void onMessage(UserEventDto userEvent) {
        // 保存或更新用户本地数据库
        User user = new User();
        user.setId(userEvent.getId());
        user.setUsername(userEvent.getUsername());
        user.setEmail(userEvent.getEmail());
        user.setRole(userEvent.getRole());

        userRepository.save(user);
    }
}
