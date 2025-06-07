package com.easyride.analytics_service.rocket;

import com.easyride.analytics_service.dto.AnalyticsRequestDto;
import com.easyride.analytics_service.dto.OrderCompletedEvent; // Assuming this is still a relevant DTO
// OR: Consume a more generic OrderStatusChangedEvent if Order Service publishes that for cancellations etc.
// import com.easyride.analytics_service.dto.OrderStatusChangedEventDto;
import com.easyride.analytics_service.model.RecordType;
import com.easyride.analytics_service.service.AnalyticsService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RocketMQMessageListener(
        topic = "order-topic",
        consumerGroup = "${rocketmq.consumer.group}",
        // Listen to more order events for richer analytics
        selectorExpression = "ORDER_COMPLETED || ORDER_PAYMENT_SETTLED || ORDER_CANCELLED || ORDER_CREATED || DRIVER_ASSIGNED"
)
// Use a more generic event DTO if Order Service emits one, or handle multiple DTO types
public class OrderEventListener implements RocketMQListener<Object> {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @Autowired
    private AnalyticsService analyticsService;

    @Override
    public void onMessage(Object message) { // Changed to Object to handle multiple types
        log.info("Received message from order-topic: {}", message.getClass().getSimpleName());

        if (message instanceof OrderCompletedEvent event) { // Assuming this is what payment_settled means for analytics
            processOrderCompletedOrSettled(event);
        } else if (message instanceof com.easyride.analytics_service.dto.OrderStatusChangedEventDto event) { // Define this DTO
            processOrderStatusChanged(event);
        }
        // Add more `else if` for other specific DTOs from order-topic if needed
    }

    private void processOrderCompletedOrSettled(OrderCompletedEvent event) {
        log.info("Processing OrderCompletedEvent for analytics: {}", event);
        try {
            // Metric: Order Revenue
            AnalyticsRequestDto revenueRequest = new AnalyticsRequestDto();
            revenueRequest.setRecordType(RecordType.ORDER_REVENUE.name());
            revenueRequest.setMetricName("order_revenue_total");
            revenueRequest.setMetricValue(event.getFinalAmount()); // From OrderCompletedEvent
            revenueRequest.setRecordTime(event.getOrderCompletionTime() != null ? event.getOrderCompletionTime() : LocalDateTime.now());
            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("orderId", String.valueOf(event.getOrderId()));
            dimensions.put("passengerId", String.valueOf(event.getPassengerId()));
            dimensions.put("driverId", String.valueOf(event.getDriverId()));
            dimensions.put("serviceType", event.getServiceType());
            dimensions.put("vehicleType", event.getVehicleType());
            revenueRequest.setDimensions(dimensions);
            analyticsService.recordAnalyticsData(revenueRequest);

            // Metric: Completed Orders Count
            AnalyticsRequestDto countRequest = new AnalyticsRequestDto();
            countRequest.setRecordType(RecordType.COMPLETED_ORDERS_COUNT.name());
            countRequest.setMetricName("completed_orders_count");
            countRequest.setMetricValue(1.0);
            countRequest.setRecordTime(revenueRequest.getRecordTime());
            countRequest.setDimensions(new HashMap<>(dimensions)); // Copy dimensions
            analyticsService.recordAnalyticsData(countRequest);

        } catch (Exception e) {
            log.error("Error processing OrderCompletedEvent for analytics: ", e);
        }
    }

    private void processOrderStatusChanged(com.easyride.analytics_service.dto.OrderStatusChangedEventDto event) {
        log.info("Processing OrderStatusChangedEvent: {}", event);
        try {
            AnalyticsRequestDto analyticsRequest = new AnalyticsRequestDto();
            analyticsRequest.setRecordTime(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now());
            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("orderId", String.valueOf(event.getOrderId()));
            dimensions.put("userId", String.valueOf(event.getUserId())); // User who triggered change, if relevant
            dimensions.put("userRole", event.getUserRole());
            // Add more dimensions like original status, reason for cancellation etc.
            analyticsRequest.setDimensions(dimensions);

            if ("ORDER_CANCELLED".equalsIgnoreCase(event.getNewStatus())) { // Assuming newStatus field in event
                analyticsRequest.setRecordType(RecordType.CANCELLED_ORDERS_COUNT.name());
                analyticsRequest.setMetricName("cancelled_order_count");
                analyticsRequest.setMetricValue(1.0);
                analyticsService.recordAnalyticsData(analyticsRequest);
            } else if ("ORDER_CREATED".equalsIgnoreCase(event.getNewStatus())) {
                analyticsRequest.setRecordType(RecordType.ORDER_REQUEST.name());
                analyticsRequest.setMetricName("order_request_count");
                analyticsRequest.setMetricValue(1.0);
                analyticsService.recordAnalyticsData(analyticsRequest);
            } else if ("DRIVER_ASSIGNED".equalsIgnoreCase(event.getNewStatus()) || "ACCEPTED".equalsIgnoreCase(event.getNewStatus())) {
                // This might be better if MatchService sends an "ORDER_ACCEPTED_BY_DRIVER" event directly to analytics
                // Or OrderService relays this with driverId.
                analyticsRequest.setRecordType(RecordType.ORDER_ACCEPTED_BY_DRIVER.name());
                analyticsRequest.setMetricName("order_accepted_by_driver_count");
                analyticsRequest.setMetricValue(1.0);
                if (event.getDriverId() != null) dimensions.put("driverId", String.valueOf(event.getDriverId()));
                analyticsService.recordAnalyticsData(analyticsRequest);
            }
            // Add other status changes relevant for metrics
        } catch (Exception e) {
            log.error("Error processing OrderStatusChangedEvent for analytics: ", e);
        }
    }
}