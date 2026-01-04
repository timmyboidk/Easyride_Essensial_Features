package com.easyride.notification_service.service;

import com.easyride.notification_service.dto.*;
import com.easyride.notification_service.model.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationDispatcherService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcherService.class);

    private final TemplateService templateService;
    private final SmsNotificationService smsService;
    private final EmailNotificationService emailService;
    private final PushNotificationService pushService;

    public NotificationDispatcherService(TemplateService templateService,
            SmsNotificationService smsService,
            EmailNotificationService emailService,
            PushNotificationService pushService) {
        this.templateService = templateService;
        this.smsService = smsService;
        this.emailService = emailService;
        this.pushService = pushService;
    }

    // Example for Order Events
    public void dispatchOrderNotification(ConsumedOrderEventDto event) {
        log.info("Dispatching notification for order event: {} for orderId {}", event.getEventType(),
                event.getOrderId());

        String templateKey = determineTemplateKeyForOrderEvent(event.getEventType());
        if (templateKey == null) {
            log.warn("No template key defined for order event type: {}", event.getEventType());
            return;
        }

        Map<String, Object> model = new HashMap<>(
                event.getAdditionalData() != null ? event.getAdditionalData() : Map.of());
        model.put("orderId", event.getOrderId());
        model.put("passengerId", event.getPassengerId());
        model.put("driverId", event.getDriverId());
        // Add more common fields to model if needed

        String userLocale = event.getPassengerLocale() != null ? event.getPassengerLocale() : "en_US"; // Default locale

        // --- SMS ---
        if (event.getPassengerPhoneNumber() != null /* && passenger.prefersSmsForThisEvent() */) {
            String smsMessage = templateService.processTemplate(templateKey, NotificationChannel.SMS, userLocale,
                    model);
            if (smsMessage != null && !smsMessage.startsWith("Error:")) {
                smsService.sendSms(event.getPassengerPhoneNumber(), smsMessage);
            }
        }

        // --- Email ---
        if (event.getPassengerEmail() != null /* && passenger.prefersEmailForThisEvent() */) {
            String emailSubject = templateService.processTemplate(templateKey, NotificationChannel.EMAIL_SUBJECT,
                    userLocale, model);
            String emailBody = templateService.processTemplate(templateKey, NotificationChannel.EMAIL_BODY, userLocale,
                    model);
            if (emailSubject != null && !emailSubject.startsWith("Error:") && emailBody != null
                    && !emailBody.startsWith("Error:")) {
                emailService.sendEmail(event.getPassengerEmail(), emailSubject, emailBody);
            }
        }

        // --- Push Notification ---
        // Construct common push payload details
        String pushTitle = templateService.processTemplate(templateKey + "_title", NotificationChannel.PUSH_APNS,
                userLocale, model); // or PUSH_FCM
        String pushBody = templateService.processTemplate(templateKey + "_body", NotificationChannel.PUSH_APNS,
                userLocale, model);

        if (pushTitle != null && !pushTitle.startsWith("Error:") && pushBody != null
                && !pushBody.startsWith("Error:")) {
            NotificationPayloadDto pushPayload = NotificationPayloadDto.builder()
                    .title(pushTitle)
                    .body(pushBody)
                    .data(Map.of("orderId", String.valueOf(event.getOrderId()), "eventType", event.getEventType())) // Example
                                                                                                                    // data
                    .build();

            if (event.getPassengerApnsToken() != null) {
                pushService.sendApnsPush(event.getPassengerApnsToken(), pushPayload);
            }
            if (event.getPassengerFcmToken() != null) {
                pushService.sendFcmPush(event.getPassengerFcmToken(), pushPayload);
            }
        }
    }

    // Add dispatch methods for other event types (Payment, User) similarly

    private String determineTemplateKeyForOrderEvent(String eventType) {
        // Map business event types to template names (without locale/channel suffix)
        return switch (eventType.toUpperCase()) {
            case "ORDER_ACCEPTED", "DRIVER_ASSIGNED" -> "order_accepted";
            case "DRIVER_ARRIVED" -> "driver_arrived";
            case "ORDER_COMPLETED" -> "order_completed";
            case "ORDER_CANCELLED_PASSENGER" -> "order_cancelled_passenger";
            case "ORDER_CANCELLED_DRIVER" -> "order_cancelled_driver";
            case "ORDER_SCHEDULED_REMINDER" -> "order_scheduled_reminder";
            // ... other mappings
            default -> null;
        };
    }

    public void dispatchOrderCreated(OrderCreatedEvent event) {
        log.info("Dispatching notification for OrderCreatedEvent: orderId={}", event.getOrderId());
    }

    public void dispatchOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Dispatching notification for OrderStatusChangedEvent: orderId={}, status={}", event.getOrderId(),
                event.getNewStatus());
    }

    public void dispatchPaymentSuccess(PaymentSuccessEvent event) {
        log.info("Dispatching notification for PaymentSuccessEvent: paymentId={}", event.getPaymentId());
    }

    public void dispatchUserRegistered(UserRegisteredEvent event) {
        log.info("Dispatching notification for UserRegisteredEvent: userId={}", event.getUserId());
    }

    public void dispatchDriverApplication(DriverApplicationEvent event) {
        log.info("Dispatching notification for DriverApplicationEvent: driverId={}", event.getDriverUserId());
    }
}