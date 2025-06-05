package com.easyride.notification_service.service;

// APNS (Pushy) imports
import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;

// FCM (Firebase) imports
import com.google.firebase.messaging.*; // FirebaseMessaging, Message, Notification etc.
import com.google.firebase.FirebaseApp; // To ensure app is initialized

import com.easyride.notification_service.dto.NotificationPayloadDto; // New DTO
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
public class PushNotificationService  {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    // APNS fields
    private final ApnsClient apnsClient;
    private final String apnsBundleId;

    // FCM fields
    private final FirebaseMessaging firebaseMessaging;


    @Autowired
    public PushNotificationService(ApnsClient apnsClient,
                                   @Value("${apns.bundleId}") String apnsBundleId,
                                   FirebaseApp firebaseApp) { // Inject FirebaseApp
        this.apnsClient = apnsClient;
        this.apnsBundleId = apnsBundleId;
        this.firebaseMessaging = FirebaseMessaging.getInstance(firebaseApp);
    }

    public void sendApnsPush(String deviceToken, NotificationPayloadDto payload) {
        final String apnsPayload = new SimpleApnsPayloadBuilder()
                .setAlertTitle(payload.getTitle())
                .setAlertBody(payload.getBody())
                .setSound("default")
                // .addCustomProperty("orderId", payload.getData().get("orderId")) // Example custom data
                .build();

        final String token = TokenUtil.sanitizeTokenString(deviceToken);
        log.info("Sending APNS push to token {}: {}", token, apnsPayload);

        final PushNotificationFuture<com.eatthepath.pushy.apns.SimpleApnsPushNotification, PushNotificationResponse<com.eatthepath.pushy.apns.SimpleApnsPushNotification>> sendNotificationFuture =
                apnsClient.sendNotification(new com.eatthepath.pushy.apns.SimpleApnsPushNotification(token, apnsBundleId, apnsPayload));

        try {
            final PushNotificationResponse<com.eatthepath.pushy.apns.SimpleApnsPushNotification> pushNotificationResponse =
                    sendNotificationFuture.get(); // Wait for response

            if (pushNotificationResponse.isAccepted()) {
                log.info("APNS Push notification accepted by APNs gateway for token: {}", deviceToken);
            } else {
                log.warn("APNS Push notification rejected by APNs gateway for token {}. Reason: {}",
                        deviceToken, pushNotificationResponse.getRejectionReason());
                pushNotificationResponse.getTokenInvalidationTimestamp().ifPresent(timestamp ->
                        log.warn("\t…and the token is invalid as of {}", timestamp));
            }
        } catch (final Exception e) {
            log.error("Failed to send APNS push notification to token {}: ", deviceToken, e);
        }
    }

    public void sendFcmPush(String deviceToken, NotificationPayloadDto payloadDto) {
        Notification notification = Notification.builder()
                .setTitle(payloadDto.getTitle())
                .setBody(payloadDto.getBody())
                .build();

        Message.Builder messageBuilder = Message.builder()
                .setNotification(notification)
                .setToken(deviceToken); // FCM registration token

        if (payloadDto.getData() != null && !payloadDto.getData().isEmpty()) {
            messageBuilder.putAllData(payloadDto.getData());
        }

        Message message = messageBuilder.build();

        try {
            log.info("Sending FCM push to token {}: Title='{}', Body='{}'", deviceToken, payloadDto.getTitle(), payloadDto.getBody());
            String response = firebaseMessaging.send(message);
            log.info("Successfully sent FCM message: " + response);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message to token {}: ", deviceToken, e);
            // Handle specific exceptions like UNREGISTERED, INVALID_ARGUMENT etc.
        }
    }
}

