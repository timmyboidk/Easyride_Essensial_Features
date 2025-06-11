package com.easyride.user_service.dto;

import lombok.Data;

@Data
public class ResetPasswordRequestDto {
    private String phoneNumber;
    private String otp;
    private String newPassword;
}