package com.easyride.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PhoneOtpLoginRequestDto {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "无效的手机号格式")
    private String phoneNumber;

    @NotBlank(message = "OTP验证码不能为空")
    private String otpCode;
}