package com.easyride.notification_service.model;

import lombok.Data;
@Data

public class PushRequest {
    private String deviceToken;
    private String payload;
    private String messageType; // 推送类型 ("push", "sms", "email")

}