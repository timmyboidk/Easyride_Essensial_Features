package com.easyride.admin_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 管理员用户名
    private String username;

    // 管理员密码（需在生产中加密或散列）
    private String password;

    // 管理员角色，如财务、客服、超级管理员等
    @Enumerated(EnumType.STRING)
    private Role role;

    // 是否启用
    private boolean enabled;

    // 其他可扩展字段，如操作日志、上次登录时间等
}
