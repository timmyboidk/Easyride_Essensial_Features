package com.easyride.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

    // 如果角色是 DRIVER，可以添加额外的字段，例如驾驶证号码、车辆信息等
    private String driverLicenseNumber;
    private String vehicleInfo;
}
