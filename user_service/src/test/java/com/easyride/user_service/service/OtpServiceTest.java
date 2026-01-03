package com.easyride.user_service.service;

import com.easyride.user_service.dto.NotificationRequestDto;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OtpServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OtpServiceImpl otpService;

    @Test
    void generateOtp_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String key = "1234567890";
        String otp = otpService.generateOtp(key);

        assertNotNull(otp);
        assertEquals(6, otp.length());
        verify(valueOperations, times(1)).set(eq("otp:login:" + key), anyString(), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void validateOtp_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String key = "1234567890";
        String otp = "123456";
        when(valueOperations.get("otp:login:" + key)).thenReturn(otp);

        assertTrue(otpService.validateOtp(key, otp));
        verify(redisTemplate, times(1)).delete("otp:login:" + key);
    }

    @Test
    void validateOtp_Failure_WrongOtp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String key = "1234567890";
        String storedOtp = "123456";
        when(valueOperations.get("otp:login:" + key)).thenReturn(storedOtp);

        assertFalse(otpService.validateOtp(key, "654321"));
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void validateOtp_Failure_NoOtp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String key = "1234567890";
        when(valueOperations.get("otp:login:" + key)).thenReturn(null);

        assertFalse(otpService.validateOtp(key, "123456"));
    }

    @Test
    void sendOtp_Success() {
        String phoneNumber = "1234567890";
        String otp = "123456";

        otpService.sendOtp(phoneNumber, otp);

        verify(rocketMQTemplate, times(1)).convertAndSend(eq("notification-topic:OTP_SMS"),
                any(NotificationRequestDto.class));
    }
}
