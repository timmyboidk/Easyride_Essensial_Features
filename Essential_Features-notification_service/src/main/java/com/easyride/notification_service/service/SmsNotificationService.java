package com.easyride.notification_service.service;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsNotificationService implements NotificationService {

    private final AmazonSNS snsClient;

    // 从配置文件读取 AWS SNS 配置信息
    @Value("${aws.sns.phone-number-prefix}")
    private String phoneNumberPrefix;  // 用于拼接国际区号前缀

    // 构造方法，初始化 SNS 客户端
    public SmsNotificationService() {
        this.snsClient = AmazonSNSClientBuilder.defaultClient();  // 使用默认凭证配置初始化 SNS 客户端
    }

    @Override
    public boolean sendNotification(String phoneNumber, String message) {
        return sendSms(phoneNumber, message);
    }

    // 发送短信的方法
    private boolean sendSms(String phoneNumber, String message) {
        try {
            // 确保手机号包含国际区号
            if (!phoneNumber.startsWith("+")) {
                phoneNumber = phoneNumberPrefix + phoneNumber;  // 添加国际区号前缀
            }

            // 创建发送请求
            PublishRequest publishRequest = new PublishRequest()
                    .withPhoneNumber(phoneNumber)  // 设置接收短信的手机号码
                    .withMessage(message);         // 设置短信内容

            // 调用 SNS 客户端发送短信
            PublishResult result = snsClient.publish(publishRequest);

            // 输出成功的日志信息
            System.out.println("Message sent successfully. Message ID: " + result.getMessageId());
            return true;
        } catch (Exception e) {
            // 捕获并打印异常信息
            e.printStackTrace();
            System.err.println("Error sending SMS: " + e.getMessage());
            return false;
        }
    }
}

