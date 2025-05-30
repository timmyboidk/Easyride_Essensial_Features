package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.WithdrawalRequestDto;
import com.easyride.payment_service.dto.WithdrawalResponseDto;
import com.easyride.payment_service.model.Wallet;
import com.easyride.payment_service.model.Withdrawal;
import com.easyride.payment_service.model.WithdrawalStatus;
import com.easyride.payment_service.repository.WalletRepository;
import com.easyride.payment_service.repository.WithdrawalRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WithdrawalServiceImpl implements WithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final WalletRepository walletRepository;

    public WithdrawalServiceImpl(WithdrawalRepository withdrawalRepository,
                                 WalletRepository walletRepository) {
        this.withdrawalRepository = withdrawalRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    public WithdrawalResponseDto requestWithdrawal(WithdrawalRequestDto withdrawalRequestDto) {
        Wallet wallet = walletRepository.findById(withdrawalRequestDto.getDriverId())
                .orElseThrow(() -> new RuntimeException("钱包不存在"));

        if (wallet.getBalance() < withdrawalRequestDto.getAmount()) {
            return new WithdrawalResponseDto(null, "FAILED", "余额不足");
        }

        // 创建提现申请
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setDriverId(withdrawalRequestDto.getDriverId());
        withdrawal.setAmount(withdrawalRequestDto.getAmount());
        withdrawal.setStatus(WithdrawalStatus.PENDING);
        withdrawal.setBankAccount(withdrawalRequestDto.getBankAccount());
        withdrawal.setRequestedAt(LocalDateTime.now());
        withdrawalRepository.save(withdrawal);

        // 冻结提现金额（可选）

        return new WithdrawalResponseDto(withdrawal.getId(), "PENDING", "提现申请已提交");
    }

    @Override
    public void processWithdrawal(Long withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("提现申请不存在"));

        // 进行风控审核
        boolean isApproved = performRiskControl(withdrawal);

        if (isApproved) {
            // 调用银行接口，处理提现
            boolean result = processBankTransfer(withdrawal);

            if (result) {
                withdrawal.setStatus(WithdrawalStatus.PROCESSED);
                withdrawal.setProcessedAt(LocalDateTime.now());
                withdrawalRepository.save(withdrawal);

                // 更新钱包余额
                Wallet wallet = walletRepository.findById(withdrawal.getDriverId())
                        .orElseThrow(() -> new RuntimeException("钱包不存在"));
                wallet.setBalance(wallet.getBalance() - withdrawal.getAmount());
                wallet.setUpdatedAt(LocalDateTime.now());
                walletRepository.save(wallet);
            } else {
                withdrawal.setStatus(WithdrawalStatus.FAILED);
                withdrawalRepository.save(withdrawal);
            }
        } else {
            withdrawal.setStatus(WithdrawalStatus.REJECTED);
            withdrawalRepository.save(withdrawal);
        }
    }

    @Override
    public List<Withdrawal> getWithdrawalHistory(Long driverId) {
        return withdrawalRepository.findByDriverId(driverId);
    }

    private boolean performRiskControl(Withdrawal withdrawal) {
        // 假设每笔提现不能超过 10000.0，超过则触发风控，返回 false
        double maxAllowedAmount = 10000.0;
        if (withdrawal.getAmount() > maxAllowedAmount) {
            // 可在此记录日志，或调用其他风控服务
            return false;
        }
        return true;
    }

    private boolean processBankTransfer(Withdrawal withdrawal) {
        // 调用银行 API，处理转账
        return true;
    }
}

