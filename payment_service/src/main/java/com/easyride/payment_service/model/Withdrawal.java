package com.easyride.payment_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long walletId;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawalStatus status;

    private String notes;

    @Column(nullable = false)
    private LocalDateTime requestTime;

    private LocalDateTime completionTime;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Version
    private Long version;

}
