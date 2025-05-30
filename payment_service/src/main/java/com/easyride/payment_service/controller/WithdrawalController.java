package com.easyride.payment_service.controller;

import com.easyride.payment_service.dto.WithdrawalRequestDto;
import com.easyride.payment_service.dto.WithdrawalResponseDto;
import com.easyride.payment_service.model.Withdrawal;
import com.easyride.payment_service.service.WithdrawalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/withdrawals")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    public WithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    // 发起提现申请
    @PostMapping("/request")
    public WithdrawalResponseDto requestWithdrawal(@Valid @RequestBody WithdrawalRequestDto withdrawalRequestDto) {
        return withdrawalService.requestWithdrawal(withdrawalRequestDto);
    }

    // 获取提现记录
    @GetMapping("/{driverId}/history")
    public List<Withdrawal> getWithdrawalHistory(@PathVariable Long driverId) {
        return withdrawalService.getWithdrawalHistory(driverId);
    }
}

