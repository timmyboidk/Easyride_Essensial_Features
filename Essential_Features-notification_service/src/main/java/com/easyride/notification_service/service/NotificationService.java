package com.easyride.notification_service.service;

public interface NotificationService {
    boolean sendNotification(String recipient, String message);
}
