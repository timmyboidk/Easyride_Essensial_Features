package com.easyride.user_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Admin extends User {

    // 管理员特有的字段

    public Admin(String username, String password, String email) {
        super(username, password, email, Role.ADMIN, true, LocalDateTime.now(), LocalDateTime.now());
    }
}
