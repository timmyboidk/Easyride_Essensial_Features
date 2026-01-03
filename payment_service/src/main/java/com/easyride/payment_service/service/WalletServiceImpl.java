package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.WalletDto;
import com.easyride.payment_service.exception.ResourceNotFoundException;
import com.easyride.payment_service.model.WalletTransaction;
import com.easyride.payment_service.repository.*;
import com.easyride.payment_service.model.Payment;
import com.easyride.payment_service.model.PaymentStatus;
import com.easyride.payment_service.model.Wallet;
import com.easyride.payment_service.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletServiceImpl(WalletRepository walletRepository, PaymentRepository paymentRepository,
            WalletTransactionRepository walletTransactionRepository) {
        this.walletRepository = walletRepository;
        this.paymentRepository = paymentRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Override
    @Transactional
    public void addFunds(Long driverId, Integer amount) {
        Wallet wallet = walletRepository.findByDriverId(driverId)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setDriverId(driverId);
                    newWallet.setBalance(0);
                    newWallet.setCurrency("USD");
                    newWallet.setCreatedAt(LocalDateTime.now());
                    newWallet.setUpdatedAt(LocalDateTime.now());
                    return newWallet;
                });

        // 计算平台服务费，假设按 10% 收取
        int serviceFee = (int) Math.round(amount * 0.10);
        wallet.setBalance(wallet.getBalance() + amount - serviceFee);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public void subtractFunds(Long driverId, Integer amount) {
        Wallet wallet = walletRepository.findByDriverId(driverId)
                .orElseThrow(() -> new RuntimeException("钱包不存在"));
        if (wallet.getBalance() < amount) {
            throw new RuntimeException("钱包余额不足");
        }
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);
    }

    @Override
    public WalletDto getWallet(Long driverId) {
        Wallet wallet = walletRepository.findByDriverId(driverId)
                .orElseThrow(() -> new RuntimeException("钱包不存在"));
        return new WalletDto(wallet.getDriverId(), wallet.getBalance(), wallet.getUpdatedAt());
    }

    @Override
    public List<Payment> getEarnings(Long driverId, LocalDateTime from, LocalDateTime to) {
        // This now correctly calls the findAll() method on the instance variable
        // 'paymentRepository'
        return paymentRepository.findAll().stream()
                .filter(payment -> driverId.equals(payment.getDriverId()))
                .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                .filter(payment -> payment.getCreatedAt() != null && !payment.getCreatedAt().isBefore(from)
                        && !payment.getCreatedAt().isAfter(to))
                .collect(Collectors.toList());
    }

    @Override
    public Page<WalletTransaction> getWalletTransactions(Long driverId, Pageable pageable) {
        Wallet wallet = walletRepository.findByDriverId(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for driver " + driverId));
        return walletTransactionRepository.findByWalletId(wallet.getId(), pageable);
    }

}
