package com.easyride.payment_service.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalResponseDto {

    private Long withdrawalId;

    private String status;

    private String message;
}

