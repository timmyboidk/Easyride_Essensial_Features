package com.easyride.notification_service.service;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class PushNotificationService implements NotificationService {

    private final ApnsClient apnsClient;

    @Value("${apns.bundleId}")
    private String bundleId;

    public PushNotificationService(ApnsClient apnsClient) {
        this.apnsClient = apnsClient;
    }

    @Override
    public boolean sendNotification(String deviceToken, String payload) {
        try {
            SimpleApnsPushNotification notification = new SimpleApnsPushNotification(
                    deviceToken, bundleId, payload);

            CompletableFuture<PushNotificationResponse<SimpleApnsPushNotification>> future =
                    apnsClient.sendNotification(notification);

            future.whenComplete((response, throwable) -> {
                if (throwable != null) {
                    // 发送失败，处理异常
                    throwable.printStackTrace();
                    return;
                }

                if (response.isAccepted()) {
                    // 推送成功
                    System.out.println("Push notification accepted by APNs.");
                } else {
                    // 推送被拒绝，处理错误原因
                    System.err.println("Notification rejected by the APNs gateway: " + response.getRejectionReason());
                    if (response.getTokenInvalidationTimestamp() != null) {
                        // 令牌无效，需要从服务器中删除
                        System.err.println("Token is invalid as of " + response.getTokenInvalidationTimestamp());
                    }
                }
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void shutdown() throws Exception {
        apnsClient.close().get(1, TimeUnit.MINUTES);
    }
}

