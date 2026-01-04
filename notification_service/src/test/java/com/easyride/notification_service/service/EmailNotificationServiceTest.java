package com.easyride.notification_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Spy
    @InjectMocks
    private EmailNotificationService emailNotificationService;

    @Test
    void sendEmail_Success() throws MessagingException {
        ReflectionTestUtils.setField(emailNotificationService, "smtpHost", "localhost");
        ReflectionTestUtils.setField(emailNotificationService, "smtpPort", "25");
        ReflectionTestUtils.setField(emailNotificationService, "username", "user");
        ReflectionTestUtils.setField(emailNotificationService, "password", "pass");

        doNothing().when(emailNotificationService).sendMimeMessage(any(MimeMessage.class));

        boolean result = emailNotificationService.sendEmail("test@example.com", "Subject", "Body");
        assertTrue(result);
    }

    @Test
    void sendEmail_Failure() throws MessagingException {
        ReflectionTestUtils.setField(emailNotificationService, "smtpHost", "localhost");
        ReflectionTestUtils.setField(emailNotificationService, "smtpPort", "25");
        ReflectionTestUtils.setField(emailNotificationService, "username", "user");
        ReflectionTestUtils.setField(emailNotificationService, "password", "pass");

        doThrow(new MessagingException("Fail")).when(emailNotificationService).sendMimeMessage(any(MimeMessage.class));

        boolean result = emailNotificationService.sendEmail("test@example.com", "Subject", "Body");
        assertFalse(result);
    }
}
