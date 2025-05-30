package com.easyride.user_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String password;

    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean enabled = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long createdBy;  // 创建者 ID

    private Long updatedBy;  // 更新者 ID


    public User(String username, String password, String email, Role role, boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
