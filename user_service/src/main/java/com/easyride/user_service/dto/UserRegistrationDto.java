package com.easyride.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDto {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @Email(message = "无效的邮箱地址")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "角色不能为空")
    private String role;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "无效的手机号格式") // Basic E.164 validation
    private String phoneNumber;

    private String otpCode; // For OTP verification during registration

    // Driver specific fields
    private String realName;
    private String idCardNumber;
    private String driverLicenseNumber;
    private String carModel;
    private String carLicensePlate;
    // Consider adding fields for document IDs if files are uploaded separately
    // private String driverLicenseDocumentId;
    // private String vehicleDocumentId;
}
