package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.WithdrawalRequestDto;
import com.easyride.payment_service.dto.WithdrawalResponseDto;
import com.easyride.payment_service.model.Wallet;
import com.easyride.payment_service.model.Withdrawal;
import com.easyride.payment_service.model.WithdrawalStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easyride.payment_service.repository.WalletMapper;
import com.easyride.payment_service.repository.WithdrawalMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WithdrawalServiceImpl implements WithdrawalService {

    private final WithdrawalMapper withdrawalMapper;
    private final WalletMapper walletMapper;

    public WithdrawalServiceImpl(WithdrawalMapper withdrawalMapper,
            WalletMapper walletMapper) {
        this.withdrawalMapper = withdrawalMapper;
        this.walletMapper = walletMapper;
    }

    @Override
    public WithdrawalResponseDto requestWithdrawal(WithdrawalRequestDto withdrawalRequestDto) {
        Wallet wallet = walletMapper.selectOne(
                new LambdaQueryWrapper<Wallet>().eq(Wallet::getDriverId, withdrawalRequestDto.getDriverId()));
        if (wallet == null) {
            throw new RuntimeException("钱包不存在");
        }

        if (wallet.getBalance() < withdrawalRequestDto.getAmount()) {
            return new WithdrawalResponseDto(null, "FAILED", "余额不足");
        }

        // 创建提现申请
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setWalletId(wallet.getId());
        withdrawal.setAmount(withdrawalRequestDto.getAmount());
        withdrawal.setStatus(WithdrawalStatus.PENDING);
        withdrawal.setNotes("Withdrawal to: " + withdrawalRequestDto.getBankAccount());
        withdrawal.setRequestTime(LocalDateTime.now());
        withdrawalMapper.insert(withdrawal);

        // 冻结提现金额（可选）

        return new WithdrawalResponseDto(withdrawal.getId(), "PENDING", "提现申请已提交");
    }

    @Override
    public void processWithdrawal(Long withdrawalId) {
        Withdrawal withdrawal = withdrawalMapper.selectById(withdrawalId);
        if (withdrawal == null) {
            throw new RuntimeException("提现申请不存在");
        }

        // 进行风控审核
        boolean isApproved = performRiskControl(withdrawal);

        if (isApproved) {
            // 调用银行接口，处理提现
            boolean result = processBankTransfer(withdrawal);

            if (result) {
                withdrawal.setStatus(WithdrawalStatus.PROCESSED);
                withdrawal.setCompletionTime(LocalDateTime.now());
                withdrawalMapper.updateById(withdrawal);

                // 更新钱包余额
                Wallet wallet = walletMapper.selectById(withdrawal.getWalletId());
                if (wallet == null) {
                    throw new RuntimeException("钱包不存在");
                }
                wallet.setBalance(wallet.getBalance() - withdrawal.getAmount());
                wallet.setUpdatedAt(LocalDateTime.now());
                walletMapper.updateById(wallet);
            } else {
                withdrawal.setStatus(WithdrawalStatus.FAILED);
                withdrawalMapper.updateById(withdrawal);
            }
        } else {
            withdrawal.setStatus(WithdrawalStatus.REJECTED);
            withdrawalMapper.updateById(withdrawal);
        }
    }

    @Override
    public List<Withdrawal> getWithdrawalHistory(Long driverId) {
        Wallet wallet = walletMapper.selectOne(new LambdaQueryWrapper<Wallet>().eq(Wallet::getDriverId, driverId));
        if (wallet == null) {
            throw new RuntimeException("钱包不存在");
        }
        return withdrawalMapper
                .selectList(new LambdaQueryWrapper<Withdrawal>().eq(Withdrawal::getWalletId, wallet.getId()));
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
