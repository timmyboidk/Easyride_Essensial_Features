package com.easyride.admin_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserDto {

    private Long id;           // 若更新时需要
    private String username;
    private String password;
    private String role;       // 对应 Role 枚举
    private boolean enabled;
}
