package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.WalletDto;
import com.easyride.payment_service.model.Payment;
import com.easyride.payment_service.model.Wallet;
import com.easyride.payment_service.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    public WalletServiceImpl(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public void addFunds(Long orderId, Integer amount) {
        // 获取订单对应的司机ID
        Long driverId = getDriverIdByOrderId(orderId);

        Wallet wallet = walletRepository.findById(driverId)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setDriverId(driverId);
                    newWallet.setBalance(0);
                    newWallet.setUpdatedAt(LocalDateTime.now());
                    return newWallet;
                });

        // 计算平台服务费，假设按 10% 收取，并四舍五入为整数
        int serviceFee = (int) Math.round(amount * 0.10);
        // 更新钱包余额：采用整数运算（单位为最小货币单位）
        wallet.setBalance(wallet.getBalance() + amount - serviceFee);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public void subtractFunds(Long orderId, Integer amount) {
        // 根据订单ID获取司机ID（示例：直接返回 orderId，实际应通过订单服务通信获取正确的司机ID）
        Long driverId = getDriverIdByOrderId(orderId);
        // 获取钱包记录
        Wallet wallet = walletRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("钱包不存在"));
        // 检查余额是否充足
        if (wallet.getBalance() < amount) {
            throw new RuntimeException("钱包余额不足");
        }
        // 扣除金额并更新更新时间
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);
    }

    @Override
    public WalletDto getWallet(Long driverId) {
        Wallet wallet = walletRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("钱包不存在"));
        return new WalletDto(wallet.getDriverId(), wallet.getBalance(), wallet.getUpdatedAt());
    }

    @Override
    public List<Payment> getEarnings(Long driverId, LocalDateTime from, LocalDateTime to) {
        // TODO: 实现查询指定时间范围内的收入记录，此处暂返回空列表
        return new ArrayList<>();
    }

    private Long getDriverIdByOrderId(Long orderId) {
        // TODO: 通过与 order_service 通信，获取订单对应的司机ID
        return orderId; // 示例实现
    }
}
