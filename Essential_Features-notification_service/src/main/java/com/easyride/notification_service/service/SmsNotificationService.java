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

    // Read AWS SNS configuration from properties file
    @Value("${aws.sns.phone-number-prefix}")
    private String phoneNumberPrefix;  // To add international prefix

    // Constructor to initialize SNS client
    public SmsNotificationService() {
        this.snsClient = AmazonSNSClientBuilder.defaultClient();  // Initialize SNS client with default credentials
    }

    @Override
    public boolean sendNotification(String phoneNumber, String message) {
        return sendSms(phoneNumber, message);
    }

    // Method to send SMS
    public boolean sendSms(String phoneNumber, String message) {
        try {
            // Ensure phone number includes international prefix
            if (!phoneNumber.startsWith("+")) {
                phoneNumber = phoneNumberPrefix + phoneNumber;  // Add international prefix
            }

            // Create send request
            PublishRequest publishRequest = new PublishRequest()
                    .withPhoneNumber(phoneNumber)  // Set recipient's phone number
                    .withMessage(message);         // Set SMS content

            // Call SNS client to send SMS
            PublishResult result = snsClient.publish(publishRequest);

            // Log success message
            System.out.println("Message sent successfully. Message ID: " + result.getMessageId());
            return true;
        } catch (Exception e) {
            // Catch and print exception
            e.printStackTrace();
            System.err.println("Error sending SMS: " + e.getMessage());
            return false;
        }
    }
}