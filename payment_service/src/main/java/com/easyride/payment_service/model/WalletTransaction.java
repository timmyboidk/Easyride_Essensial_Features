package com.easyride.payment_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wallet_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private Integer amount;

    private Long relatedOrderId;
    private Long relatedWithdrawalId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private java.time.LocalDateTime transactionDate;

    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    @Version
    private Long version;
}
