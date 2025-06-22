package com.easyride.analytics_service.rocket;

import com.easyride.analytics_service.dto.AnalyticsRequestDto;
import com.easyride.analytics_service.dto.ConsumedReviewEventDto;
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
        topic = "evaluation-topic", // Topic from Review Service
        consumerGroup = "${rocketmq.consumer.group}",
        selectorExpression = "EVALUATION_CREATED"
)
public class ReviewEventListener implements RocketMQListener<ConsumedReviewEventDto> {
    private static final Logger log = LoggerFactory.getLogger(ReviewEventListener.class);

    @Autowired
    private AnalyticsService analyticsService;

    @Override
    public void onMessage(ConsumedReviewEventDto event) {
        log.info("Received ConsumedReviewEvent: {}", event);
        try {
            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("orderId", String.valueOf(event.getOrderId()));
            dimensions.put("evaluatorId", String.valueOf(event.getEvaluatorId()));
            dimensions.put("evaluateeId", String.valueOf(event.getEvaluateeId()));

            AnalyticsRequestDto analyticsRequest = new AnalyticsRequestDto();
            analyticsRequest.setRecordTime(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now());
            analyticsRequest.setDimensions(dimensions);

            if ("EVALUATION_CREATED".equals(event.getEventType())) {
                analyticsRequest.setRecordType(RecordType.REVIEW_SUBMITTED.name());
                analyticsRequest.setMetricName("review_count");
                analyticsRequest.setMetricValue(1.0);
                analyticsService.recordAnalyticsData(analyticsRequest);

                AnalyticsRequestDto ratingScoreRequest = new AnalyticsRequestDto();
                ratingScoreRequest.setRecordTime(analyticsRequest.getRecordTime());
                ratingScoreRequest.setDimensions(new HashMap<>(dimensions));

                if ("PASSENGER".equalsIgnoreCase(event.getEvaluatorRole())) {
                    ratingScoreRequest.setRecordType(RecordType.AVERAGE_RATING_DRIVER.name());
                    ratingScoreRequest.getDimensions().put("ratedEntityType", "DRIVER");
                    ratingScoreRequest.getDimensions().put("ratedEntityId", String.valueOf(event.getEvaluateeId()));
                } else {
                    ratingScoreRequest.setRecordType(RecordType.AVERAGE_RATING_PASSENGER.name());
                    ratingScoreRequest.getDimensions().put("ratedEntityType", "PASSENGER");
                    ratingScoreRequest.getDimensions().put("ratedEntityId", String.valueOf(event.getEvaluateeId()));
                }
                ratingScoreRequest.setMetricName("rating_score_sum");
                ratingScoreRequest.setMetricValue(Double.valueOf(event.getScore()));
                analyticsService.recordAnalyticsData(ratingScoreRequest);
            } else {
                log.warn("Unhandled review event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing review event for analytics: ", e);
        }
    }
}