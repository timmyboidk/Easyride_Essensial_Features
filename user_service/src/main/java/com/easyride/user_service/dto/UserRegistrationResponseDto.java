package com.easyride.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationResponseDto {
    private Long userId;
    private String username;
    private String email;
    private String phoneNumber;
    private String role;
    private boolean enabled;
}