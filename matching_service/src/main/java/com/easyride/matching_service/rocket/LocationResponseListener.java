package com.easyride.matching_service.rocket;

import com.easyride.matching_service.dto.LocationResponseEvent; // From your existing code
import com.easyride.matching_service.dto.MatchRequestDto;
import com.easyride.matching_service.service.MatchingService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate; // Example for temporary storage
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
@RocketMQMessageListener(topic = "location-response-topic", consumerGroup = "matching-service-location-consumer-group")
public class LocationResponseListener implements RocketMQListener<LocationResponseEvent> {

    private static final Logger log = LoggerFactory.getLogger(LocationResponseListener.class);

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate; // For managing pending requests

    @Autowired
    private ObjectMapper objectMapper; // For JSON serialization

    private static final String PENDING_ORDER_GEOCODING_KEY_PREFIX = "pending_geocode_order:";

    @Override
    public void onMessage(LocationResponseEvent event) {
        log.info("Received LocationResponseEvent for orderId {} (type: {}): Address='{}'",
                event.getCorrelationId(), // Corrected: Use correlationId instead of non-existent originalRequestId
                event.getLocationType(),
                event.getFormattedAddress());

        Long orderId = Long.parseLong(event.getCorrelationId()); // Corrected: Parse string ID to Long
        String orderKey = PENDING_ORDER_GEOCODING_KEY_PREFIX + orderId;

        try {
            String cachedOrderJson = redisTemplate.opsForValue().get(orderKey);
            if (cachedOrderJson == null) {
                log.warn("No pending geocoding request found in cache for orderId: {}. Ignoring location response.", orderId);
                return;
            }
            MatchRequestDto matchRequest = objectMapper.readValue(cachedOrderJson, MatchRequestDto.class);

            if ("START".equalsIgnoreCase(event.getLocationType())) {
                matchRequest.setStartAddressFormatted(event.getFormattedAddress());
            } else if ("END".equalsIgnoreCase(event.getLocationType())) {
                matchRequest.setEndAddressFormatted(event.getFormattedAddress());
            }

            // Check if both addresses are now populated
            boolean isReadyForMatching = matchRequest.getStartAddressFormatted() != null &&
                    matchRequest.getEndAddressFormatted() != null;

            if (isReadyForMatching) {
                log.info("Both start and end locations geocoded for orderId: {}. Triggering matching.", orderId);
                redisTemplate.delete(orderKey); // Remove from cache
                matchingService.findAndAssignDriver(matchRequest); // Now call the matching logic
            } else {
                // Update the cache with the partially geocoded request
                redisTemplate.opsForValue().set(orderKey, objectMapper.writeValueAsString(matchRequest));
                log.info("Updated cached MatchRequest for orderId {} with {} location. Waiting for other location.",
                        orderId, event.getLocationType());
            }

        } catch (Exception e) {
            log.error("Error processing LocationResponseEvent for orderId {}: ", orderId, e);
        }
    }
}