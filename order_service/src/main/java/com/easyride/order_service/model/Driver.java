package com.easyride.order_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Driver extends User {
    // id 和 name/username 字段已从父类 User 继承，此处无需重复定义
    // @Id
    // private Long id;
    // private String name;

    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    private boolean available;
}

