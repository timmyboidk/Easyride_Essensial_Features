package com.easyride.location_service.service;

import com.easyride.location_service.model.LocationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class LocationService {

    @Value("${google.maps.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public LocationResponse getLocationInfo(double latitude, double longitude) {
        String url = String.format(
                "https://maps.googleapis.com/maps/api/geocode/json?latlng=%s,%s&key=%s",
                latitude, longitude, apiKey);

        // 调用谷歌地图API
        LocationResponse response = restTemplate.getForObject(url, LocationResponse.class);
        return response;
    }
}
