package com.easyride.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPayloadDto {
    private String title;
    private String body;
    private Map<String, String> data; // Custom data for push notifications
}