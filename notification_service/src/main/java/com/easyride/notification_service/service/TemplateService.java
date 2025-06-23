package com.easyride.notification_service.service;

import java.util.Map;
import com.easyride.notification_service.model.NotificationChannel; // New Enum

public interface TemplateService {
    String processTemplate(String templateName, NotificationChannel channel, String locale, Map<String, Object> model);
}