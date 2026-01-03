package com.easyride.payment_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long driverId;

    @Column(nullable = false)
    private Integer balance;

    @Column(nullable = false, length = 3)
    private String currency;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
