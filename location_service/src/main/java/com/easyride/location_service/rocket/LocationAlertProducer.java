package com.easyride.location_service.rocket;

import com.easyride.location_service.dto.RouteDeviationAlertDto;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LocationAlertProducer {
    private static final Logger log = LoggerFactory.getLogger(LocationAlertProducer.class);
    private static final String ALERT_TOPIC = "safety-alert-topic"; // Consumed by Admin/Notification

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void sendRouteDeviationAlert(RouteDeviationAlertDto alert) {
        rocketMQTemplate.convertAndSend(ALERT_TOPIC + ":ROUTE_DEVIATION", alert);
        log.info("Sent ROUTE_DEVIATION alert: {}", alert);
    }
}
