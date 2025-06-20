package com.easyride.order_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "passengers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Passenger extends User {

    @Id
    private Long id;

    private String name;
}

