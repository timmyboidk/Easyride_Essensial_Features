package com.easyride.location_service.service;

import com.easyride.location_service.model.LocationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SafetyService {

    @Autowired
    private AlertService alertService;

    private final Map<Long, Integer> deviationCountMap = new ConcurrentHashMap<>();

    public void checkRouteDeviation(Long orderId, Location current, List<Location> route) {
        if (isRouteDeviated(current, route, 0.5)) {
            int count = deviationCountMap.getOrDefault(orderId, 0) + 1;
            deviationCountMap.put(orderId, count);

            if (count >= 3) {
                alertService.sendRouteDeviationAlert(orderId, current);
                deviationCountMap.remove(orderId);
            }
        } else {
            deviationCountMap.remove(orderId);
        }
    }

    private boolean isRouteDeviated(Location current, List<Location> route, double thresholdMiles) {
        // Haversine 距离计算实现
    }
}