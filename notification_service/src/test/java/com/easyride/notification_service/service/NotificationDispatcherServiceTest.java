package com.easyride.notification_service.service;

import com.easyride.notification_service.dto.ConsumedOrderEventDto;
import com.easyride.notification_service.dto.NotificationPayloadDto;
import com.easyride.notification_service.model.NotificationChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherServiceTest {

        @Mock
        private TemplateService templateService;
        @Mock
        private SmsNotificationService smsService;
        @Mock
        private EmailNotificationService emailService;
        @Mock
        private PushNotificationService pushService;

        @InjectMocks
        private NotificationDispatcherService dispatcherService;

        @Test
        void dispatchOrderNotification_Success_AllChannels() {
                ConsumedOrderEventDto event = new ConsumedOrderEventDto();
                event.setOrderId(100L);
                event.setEventType("DRIVER_ARRIVED");
                event.setPassengerPhoneNumber("1234567890");
                event.setPassengerEmail("test@example.com");
                event.setPassengerApnsToken("apns_token");
                event.setPassengerLocale("en_US");

                // Mock TemplateService responses
                when(templateService.processTemplate(eq("driver_arrived"), eq(NotificationChannel.SMS), anyString(),
                                anyMap()))
                                .thenReturn("Driver is here");
                when(templateService.processTemplate(eq("driver_arrived"), eq(NotificationChannel.EMAIL_SUBJECT),
                                anyString(),
                                anyMap()))
                                .thenReturn("Your driver has arrived");
                when(templateService.processTemplate(eq("driver_arrived"), eq(NotificationChannel.EMAIL_BODY),
                                anyString(),
                                anyMap()))
                                .thenReturn("Body content");
                when(templateService.processTemplate(eq("driver_arrived_title"), eq(NotificationChannel.PUSH_APNS),
                                anyString(),
                                anyMap()))
                                .thenReturn("Driver Arrived");
                when(templateService.processTemplate(eq("driver_arrived_body"), eq(NotificationChannel.PUSH_APNS),
                                anyString(),
                                anyMap()))
                                .thenReturn("Push body");

                dispatcherService.dispatchOrderNotification(event);

                verify(smsService).sendSms("1234567890", "Driver is here");
                verify(emailService).sendEmail("test@example.com", "Your driver has arrived", "Body content");
                verify(pushService).sendApnsPush(eq("apns_token"), any(NotificationPayloadDto.class));
        }

        @Test
        void dispatchOrderNotification_UnknownEventType_DoesNothing() {
                ConsumedOrderEventDto event = new ConsumedOrderEventDto();
                event.setEventType("UNKNOWN_EVENT");

                dispatcherService.dispatchOrderNotification(event);

                verifyNoInteractions(smsService, emailService, pushService);
        }

        @Test
        void dispatchOrderNotification_TemplateError_DoesNotSend() {
                ConsumedOrderEventDto event = new ConsumedOrderEventDto();
                event.setEventType("ORDER_ACCEPTED");
                event.setPassengerPhoneNumber("1234567890");

                when(templateService.processTemplate(anyString(), any(), anyString(), anyMap()))
                                .thenReturn("Error: Template not found");

                dispatcherService.dispatchOrderNotification(event);

                verify(smsService, never()).sendSms(anyString(), anyString());
        }
}
