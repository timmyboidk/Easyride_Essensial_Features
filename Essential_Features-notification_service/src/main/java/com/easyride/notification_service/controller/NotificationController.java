package com.easyride.notification_service.controller;

import com.easyride.notification_service.service.EmailNotificationService;
import com.easyride.notification_service.service.NotificationService;
import com.easyride.notification_service.service.PushNotificationService;
import com.easyride.notification_service.service.SmsNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

    private final NotificationService emailNotificationService;
    private final NotificationService smsNotificationService;
    private final NotificationService pushNotificationService;

    @Autowired
    public NotificationController(EmailNotificationService emailNotificationService,
                                  SmsNotificationService smsNotificationService,
                                  PushNotificationService pushNotificationService) {
        this.emailNotificationService = emailNotificationService;
        this.smsNotificationService = smsNotificationService;
        this.pushNotificationService = pushNotificationService;
    }

    // 发送短信通知
    @PostMapping("/send-sms")
    public String sendSms(@RequestParam String phoneNumber, @RequestParam String message) {
        boolean result = smsNotificationService.sendNotification(phoneNumber, message);
        if (result) {
            return "SMS sent successfully!";
        } else {
            return "Failed to send SMS.";
        }
    }

    // 发送邮件通知
    @PostMapping("/send-email")
    public String sendEmail(@RequestParam String toEmail, @RequestParam String message) {
        boolean result = emailNotificationService.sendNotification(toEmail, message);
        if (result) {
            return "Email sent successfully!";
        } else {
            return "Failed to send Email.";
        }
    }

    // 发送推送通知
    @PostMapping("/send-push")
    public String sendPush(@RequestParam String deviceToken, @RequestParam String message) {
        boolean result = pushNotificationService.sendNotification(deviceToken, message);
        if (result) {
            return "Push notification sent successfully!";
        } else {
            return "Failed to send Push notification.";
        }
    }
}

