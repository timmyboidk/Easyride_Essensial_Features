package com.easyride.order_service.rocket;

import com.easyride.order_service.dto.UserEventDto;
import com.easyride.order_service.model.User;
import com.easyride.order_service.repository.UserRepository;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RocketMQMessageListener(topic = "user-topic", consumerGroup = "order-service-group")
public class UserRocketConsumer {

    private final UserRepository userRepository;

    public UserRocketConsumer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    @Override
    public void onMessage(UserEventDto userEvent) {
     try {
            log.info("[OrderService] Received UserEvent: {}", userEvent);
            // 根据接收到的用户事件更新本地用户信息
            User user = new User();
            user.setId(userEvent.getId());
            user.setUsername(userEvent.getUsername());
            user.setEmail(userEvent.getEmail());
            user.setRole(userEvent.getRole());
            userRepository.save(user);
            log.info("[OrderService] User data updated successfully for userId: {}", userEvent.getId());
        } catch (Exception e) {
            log.error("[OrderService] Error processing UserEvent: {}", userEvent, e);
            // 可在此进行重试或发送告警
            throw e;  // 或根据业务需要处理异常
        }
    }
}
