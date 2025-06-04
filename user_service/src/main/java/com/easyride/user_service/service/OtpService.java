package com.easyride.user_service.service;

public interface OtpService {
    String generateOtp(String key); // Key could be phone number
    boolean validateOtp(String key, String otp);
    void sendOtp(String phoneNumber, String otp); // Integration with Notification Service
}