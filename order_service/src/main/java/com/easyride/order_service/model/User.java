// User.java
package com.easyride.order_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    private Long id;

    private String username;

    private String email;

    private String role;
}
