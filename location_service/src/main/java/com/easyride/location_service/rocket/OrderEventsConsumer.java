package com.easyride.location_service.rocket;

import com.easyride.location_service.dto.OrderStartedEventDto; // Define this based on what Order Service publishes
import com.easyride.location_service.dto.OrderTerminatedEventDto; // Define this for when trip ends/cancels
import com.easyride.location_service.service.SafetyService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
// This listener needs to know the planned route when a trip starts.
// OrderService should publish an event like "TRIP_STARTED_WITH_ROUTE"
@RocketMQMessageListener(topic = "EASYRIDE_ORDER_STATUS_CHANGED_TOPIC", consumerGroup = "CID_LOCATION_SERVICE", selectorExpression = "TRIP_STARTED_WITH_ROUTE || TRIP_ENDED || ORDER_CANCELLED_ACTIVE_TRIP")
public class OrderEventsConsumer implements RocketMQListener<Object> {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsConsumer.class);

    @Autowired
    private SafetyService safetyService;

    @Override
    public void onMessage(Object message) {
        try {
            if (message instanceof OrderStartedEventDto event) {
                log.info("Received OrderStartedEvent with route for order {}", event.getOrderId());
                if (event.getPlannedRoute() != null) {
                    safetyService.storePlannedRoute(event.getPlannedRoute());
                } else {
                    log.warn("OrderStartedEvent for order {} did not contain a planned route.", event.getOrderId());
                }
            } else if (message instanceof OrderTerminatedEventDto event) { // For trip end/cancellation
                log.info("Received OrderTerminatedEvent for order {}, removing planned route.", event.getOrderId());
                safetyService.removePlannedRoute(event.getOrderId());
            } else {
                log.warn("Received unknown message type from order-topic: {}", message.getClass().getName());
            }
        } catch (Exception e) {
            log.error("Error processing message from order-topic: ", e);
        }
    }
}