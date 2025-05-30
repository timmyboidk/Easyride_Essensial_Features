package com.easyride.order_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Driver {

    @Id
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    private boolean available;
}

