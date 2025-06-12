package com.easyride.user_service.model;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Passenger extends User {
    public Passenger(String username, String password, String email, String phoneNumber) {
        super(username, password, email, phoneNumber, Role.PASSENGER);
    }
}