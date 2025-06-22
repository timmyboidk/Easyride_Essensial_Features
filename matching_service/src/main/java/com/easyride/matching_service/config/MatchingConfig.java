package com.easyride.matching_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "matching")
@Data
public class MatchingConfig {
    private double maxMatchRadiusKm;
    private int maxDriverWorkHours;
    private double ratingWeight;
    private double distanceWeight;
    private int carpoolMaxPassengers;
}