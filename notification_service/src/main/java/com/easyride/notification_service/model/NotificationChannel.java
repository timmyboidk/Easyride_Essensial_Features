package com.easyride.notification_service.model;

public enum NotificationChannel {
    SMS,
    EMAIL_SUBJECT, // For email subject lines
    EMAIL_BODY,    // For email body content
    PUSH_APNS,     // For APNS push content (title/body structure might be handled by payload DTO)
    PUSH_FCM       // For FCM push content
}