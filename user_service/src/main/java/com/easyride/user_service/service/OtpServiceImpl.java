package com.easyride.user_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.apache.rocketmq.spring.core.RocketMQTemplate; // For sending OTP via MQ to NotificationService
import com.easyride.user_service.dto.NotificationRequestDto; // Define this DTO
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);
    private static final long OTP_VALIDITY_MINUTES = 5;
    private final StringRedisTemplate redisTemplate;
    private final RocketMQTemplate rocketMQTemplate;

    @Autowired
    public OtpServiceImpl(StringRedisTemplate redisTemplate, RocketMQTemplate rocketMQTemplate) {
        this.redisTemplate = redisTemplate;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Override
    public String generateOtp(String key) {
        SecureRandom random = new SecureRandom();
        String otp = String.format("%06d", random.nextInt(999999));
        redisTemplate.opsForValue().set("otp:" + key, otp, OTP_VALIDITY_MINUTES, TimeUnit.MINUTES);
        log.info("Generated OTP for key {}: {}", key, otp); // Be careful logging OTPs even in info
        return otp;
    }

    @Override
    public boolean validateOtp(String key, String otpToValidate) {
        String storedOtp = redisTemplate.opsForValue().get("otp:" + key);
        if (storedOtp != null && storedOtp.equals(otpToValidate)) {
            redisTemplate.delete("otp:" + key); // OTP is single-use
            log.info("OTP validated successfully for key {}", key);
            return true;
        }
        log.warn("OTP validation failed for key {}. Submitted OTP: {}", key, otpToValidate);
        return false;
    }

    @Override
    public void sendOtp(String phoneNumber, String otp) {
        // This should ideally call the Notification Service
        // Option 1: Feign Client (you'd need to define NotificationServiceClient)
        // notificationServiceClient.sendSms(new SmsRequest(phoneNumber, "Your OTP is: " + otp));

        // Option 2: Send a message to Notification Service via RocketMQ
        String message = "Your EasyRide OTP is: " + otp + ". It is valid for " + OTP_VALIDITY_MINUTES + " minutes.";
        NotificationRequestDto notificationRequest = new NotificationRequestDto(phoneNumber, message, "SMS");
        // Assuming "notification-topic" and NotificationRequestDto are defined
        // The tag "OTP_SMS" can be used by NotificationService to route to SMS sending logic.
        rocketMQTemplate.convertAndSend("notification-topic:OTP_SMS", notificationRequest);
        log.info("OTP sending request queued for phone number: {}", phoneNumber);
    }
}