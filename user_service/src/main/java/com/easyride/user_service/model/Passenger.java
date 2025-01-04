package com.easyride.user_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "passengers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Passenger extends User {

    // 乘客特有的字段

    public Passenger(String username, String password, String email) {
        super(username, password, email, Role.PASSENGER, true, LocalDateTime.now(), LocalDateTime.now());
    }
}

