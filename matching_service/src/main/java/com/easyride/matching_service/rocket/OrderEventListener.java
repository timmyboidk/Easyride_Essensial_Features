package com.easyride.matching_service.rocket;

import com.easyride.matching_service.dto.LocationRequestEvent;
import com.easyride.matching_service.dto.MatchRequestDto;
import com.easyride.matching_service.dto.OrderCreatedEvent; //  incoming event from Order Service
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RocketMQMessageListener(
        topic = "order-topic",
        consumerGroup = "matching-service-order-consumer-group",
        selectorExpression = "ORDER_CREATED" // Only listen to new orders
)
public class OrderEventListener implements RocketMQListener<OrderCreatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    // private final MatchingService matchingService; // No longer directly call matching logic here
    private final RocketMQTemplate rocketMQTemplate; // To send LocationRequestEvents
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;


    private static final String PENDING_ORDER_GEOCODING_KEY_PREFIX = "pending_geocode_order:";
    private static final String LOCATION_REQUEST_TOPIC = "location-request-topic";


    @Autowired
    public OrderEventListener(RocketMQTemplate rocketMQTemplate,
                              RedisTemplate<String, String> redisTemplate,
                              ObjectMapper objectMapper) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(OrderCreatedEvent orderEvent) {
        log.info("Received OrderCreatedEvent for order ID: {}", orderEvent.getOrderId());

        // Convert OrderCreatedEvent to MatchRequestDto (or use it directly if fields align)
        MatchRequestDto matchRequest = new MatchRequestDto(
                orderEvent.getOrderId(),
                orderEvent.getPassengerId(),
                orderEvent.getStartLatitude(),
                orderEvent.getStartLongitude(),
                null, // Start address to be geocoded
                orderEvent.getEndLatitude(),
                orderEvent.getEndLongitude(),
                null, // End address to be geocoded
                orderEvent.getVehicleType(), // Corrected: Removed .name()
                orderEvent.getServiceType(), // Corrected: Removed .name()
                orderEvent.getEstimatedCost(),
                orderEvent.getScheduledTime(),
                orderEvent.getOrderTime(),
                orderEvent.getPassengerRating(), // Add these to OrderCreatedEvent if available
                orderEvent.getPreferredDriverTags(),
                orderEvent.getPreferredDriverId()
        );

        try {
            // Store the initial request in Redis
            String orderKey = PENDING_ORDER_GEOCODING_KEY_PREFIX + orderEvent.getOrderId();
            redisTemplate.opsForValue().set(orderKey, objectMapper.writeValueAsString(matchRequest));
            log.info("Cached initial MatchRequest for orderId {}. Requesting geocoding.", orderEvent.getOrderId());

            // Send LocationRequestEvents for start and end locations
            LocationRequestEvent startLocReq = new LocationRequestEvent(
                    orderEvent.getOrderId().toString(), // Corrected: Use toString() for correlationId
                    "START",
                    orderEvent.getStartLatitude(),
                    orderEvent.getStartLongitude()
            );
            rocketMQTemplate.convertAndSend(LOCATION_REQUEST_TOPIC, startLocReq);
            log.info("Sent LocationRequestEvent for START location of orderId {}", orderEvent.getOrderId());

            LocationRequestEvent endLocReq = new LocationRequestEvent(
                    orderEvent.getOrderId().toString(), // Corrected: Use toString() for correlationId
                    "END",
                    orderEvent.getEndLatitude(),
                    orderEvent.getEndLongitude()
            );
            rocketMQTemplate.convertAndSend(LOCATION_REQUEST_TOPIC, endLocReq);
            log.info("Sent LocationRequestEvent for END location of orderId {}", orderEvent.getOrderId());

        } catch (Exception e) {
            log.error("Error processing OrderCreatedEvent and initiating geocoding for orderId {}: ", orderEvent.getOrderId(), e);
            // Handle error, potentially remove from Redis or mark as failed
        }
    }
}