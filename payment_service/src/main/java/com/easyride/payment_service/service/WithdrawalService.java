package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.WithdrawalRequestDto;
import com.easyride.payment_service.dto.WithdrawalResponseDto;
import com.easyride.payment_service.model.Withdrawal;

import java.util.List;

public interface WithdrawalService {

    WithdrawalResponseDto requestWithdrawal(WithdrawalRequestDto withdrawalRequestDto);

    void processWithdrawal(Long withdrawalId);

    List<Withdrawal> getWithdrawalHistory(Long driverId);
}

