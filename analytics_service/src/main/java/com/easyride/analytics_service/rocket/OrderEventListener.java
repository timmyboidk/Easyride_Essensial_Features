package com.easyride.analytics_service.rocket;

import com.easyride.analytics_service.dto.OrderCompletedEvent;
import com.easyride.analytics_service.model.AnalyticsRecord;
import com.easyride.analytics_service.model.RecordType;
import com.easyride.analytics_service.service.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 监听 order-topic，接收来自 order_service 的订单完成等事件
 */
@Slf4j
@Service
@RocketMQMessageListener(
    topic = "order-topic",
    consumerGroup = "analytics-consumer-group",
    // selectorExpression = "ORDER_COMPLETED" // 如果只想监听带有此 Tag 的消息可加
    selectorExpression = "*"
)
public class OrderEventListener implements RocketMQListener<OrderCompletedEvent> {

    private final AnalyticsService analyticsService;

    public OrderEventListener(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Override
    public void onMessage(OrderCompletedEvent event) {
        log.info("[Analytics] Received ORDER_COMPLETED event: {}", event);

        // 解析事件，构造 AnalyticsRecord
        // event 中可能包含订单ID、乘客ID、金额、完成时间、区域等
        AnalyticsRecord record = AnalyticsRecord.builder()
            .recordType(RecordType.ORDER_DATA)
            .metricName("orderRevenue")
            .metricValue(event.getOrderAmount())
            .recordTime(LocalDateTime.now()) // 或 event.getCompletedTime()
            .dimensionKey("region")  // 示例：维度为区域
            .dimensionValue(event.getRegion()) // 具体区域
            .build();

        // 调用 analyticsService 写入数据库
        analyticsService.recordAnalyticsData(record);

        // 也可同时写入其他指标，如订单完成数
        AnalyticsRecord countRecord = AnalyticsRecord.builder()
            .recordType(RecordType.ORDER_DATA)
            .metricName("completedOrdersCount")
            .metricValue(1.0)
            .recordTime(LocalDateTime.now())
            .build();
        analyticsService.recordAnalyticsData(countRecord);
    }
}
