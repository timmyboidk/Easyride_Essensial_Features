package com.easyride.notification_service.controller;

import com.easyride.notification_service.service.EmailNotificationService;
import com.easyride.notification_service.service.PushNotificationService;
import com.easyride.notification_service.service.SmsNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmailNotificationService emailNotificationService;

    @MockBean
    private SmsNotificationService smsNotificationService;

    @MockBean
    private PushNotificationService pushNotificationService;

    @Test
    void sendSms_Success() throws Exception {
        when(smsNotificationService.sendNotification(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/send-sms")
                .param("phoneNumber", "1234567890")
                .param("message", "Test SMS"))
                .andExpect(status().isOk())
                .andExpect(content().string("SMS sent successfully!"));
    }

    @Test
    void sendEmail_Success() throws Exception {
        when(emailNotificationService.sendNotification(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/send-email")
                .param("toEmail", "test@example.com")
                .param("message", "Test Email"))
                .andExpect(status().isOk())
                .andExpect(content().string("Email sent successfully!"));
    }

    @Test
    void sendPush_Success() throws Exception {
        when(pushNotificationService.sendNotification(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/send-push")
                .param("deviceToken", "token123")
                .param("message", "Test Push"))
                .andExpect(status().isOk())
                .andExpect(content().string("Push notification sent successfully!"));
    }

    @Test
    void sendSms_Failure() throws Exception {
        when(smsNotificationService.sendNotification(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/send-sms")
                .param("phoneNumber", "1234567890")
                .param("message", "Fail SMS"))
                .andExpect(status().isOk())
                .andExpect(content().string("Failed to send SMS."));
    }
}
