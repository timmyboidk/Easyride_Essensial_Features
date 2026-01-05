package com.easyride.payment_service.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("withdrawals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Withdrawal {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long walletId;

    private Integer amount;

    private WithdrawalStatus status;

    private String notes;

    private LocalDateTime requestTime;

    private LocalDateTime completionTime;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Version
    private Long version;

}
