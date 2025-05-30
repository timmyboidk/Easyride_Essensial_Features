package com.easyride.user_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Driver extends User {

    // 司机特有的字段

    public Driver(String username, String password, String email) {
        super(username, password, email, Role.DRIVER, true, LocalDateTime.now(), LocalDateTime.now());
    }
}
