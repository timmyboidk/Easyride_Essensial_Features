package com.easyride.matching_service.rocket;

import com.easyride.matching_service.dto.MatchRequestDto;
import com.easyride.matching_service.dto.OrderCreatedEvent;
import com.easyride.matching_service.service.MatchingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

/**
 * 监听 order-topic 的 ORDER_CREATED 事件
 */
@Slf4j
@Service
@RocketMQMessageListener(topic = "order-topic", consumerGroup = "matching-service-group")
public class OrderEventListener implements RocketMQListener<OrderCreatedEvent> {

    private final MatchingService matchingService;

    public OrderEventListener(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @Override
    public void onMessage(OrderCreatedEvent event) {
        log.info("[MatchingService] Received OrderCreatedEvent: {}", event);

        // 1. 转为 MatchRequestDto
        MatchRequestDto request = MatchRequestDto.builder()
                .orderId(event.getOrderId())
                .passengerId(event.getPassengerId())
                .startLatitude(event.getStartLatitude())
                .startLongitude(event.getStartLongitude())
                .vehicleType(event.getVehicleType())
                .serviceType(event.getServiceType())
                .paymentMethod(event.getPaymentMethod())
                .estimatedCost(event.getEstimatedCost())
                .build();

        // 2. 自动匹配
        Long bestDriverId = matchingService.matchDriver(request);
        if (bestDriverId != null) {
            log.info("Auto-match success, bestDriverId = {}", bestDriverId);
            // 3. 通知 order_service 或立即调用 matchingService.acceptOrder(...)?
            //    取决于设计，如果是自动分配不需要司机确认，可直接 acceptOrder
        } else {
            log.warn("No suitable driver found for order {}", event.getOrderId());
        }
    }
}
