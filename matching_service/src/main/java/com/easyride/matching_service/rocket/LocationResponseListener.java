package com.easyride.matching_service.rocket;

import com.easyride.matching_service.dto.LocationResponseEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

/**
 * 监听 location-topic 返回的地理信息
 */
@Slf4j
@Service
@RocketMQMessageListener(
        topic = "location-response-topic",
        consumerGroup = "matching-service-location-group"
)
public class LocationResponseListener implements RocketMQListener<LocationResponseEvent> {

    // 若需要根据 correlationId 找到对应请求,
    // 可以使用 ConcurrentHashMap 等缓存中间结果
    // 或者数据库记录.

    @Override
    public void onMessage(LocationResponseEvent event) {
        log.info("[MatchingService] Received LocationResponseEvent: {}", event);

        // 1. 根据 event.getCorrelationId() 找到原先发出请求的场景
        // 2. 执行后续逻辑，如将地址信息写入 MatchingRecord 或更新某个 driverStatus

        // 示例仅打印
        String address = event.getFormattedAddress();
        log.info("Got address for correlationId={} : {}", event.getCorrelationId(), address);
    }
}
