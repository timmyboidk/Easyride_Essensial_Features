package com.easyride.payment_service.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRequestDto {

    private Long driverId;

    // 修改为 Integer 类型，单位为最小货币单位，例如 100 分表示 1 元
    private Integer amount;

    private String bankAccount;
}

