package com.easyride.payment_service.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@TableName("wallet_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long walletId;

    private TransactionType type;

    private Integer amount;

    private Long relatedOrderId;
    private Long relatedWithdrawalId;

    private String status;

    private java.time.LocalDateTime transactionDate;

    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    @Version
    private Long version;
}
