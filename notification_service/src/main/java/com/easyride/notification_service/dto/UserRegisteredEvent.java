package com.easyride.notification_service.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserRegisteredEvent implements Serializable {
    private Long userId;
    private String role; // PASSENGER or DRIVER
    private String phoneNumber;
    private String email;
    private LocalDateTime registrationTime;
}
