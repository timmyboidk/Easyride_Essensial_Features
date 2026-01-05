package com.easyride.payment_service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easyride.payment_service.dto.WalletDto;
import com.easyride.payment_service.exception.ResourceNotFoundException;
import com.easyride.payment_service.model.*;
import com.easyride.payment_service.repository.*;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletMapper walletMapper;
    private final PaymentMapper paymentMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    public WalletServiceImpl(WalletMapper walletMapper, PaymentMapper paymentMapper,
            WalletTransactionMapper walletTransactionMapper) {
        this.walletMapper = walletMapper;
        this.paymentMapper = paymentMapper;
        this.walletTransactionMapper = walletTransactionMapper;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void addFunds(Long driverId, Integer amount) {
        Wallet wallet = walletMapper.selectOne(new LambdaQueryWrapper<Wallet>().eq(Wallet::getDriverId, driverId));
        boolean exist = (wallet != null);
        if (!exist) {
            wallet = new Wallet();
            wallet.setDriverId(driverId);
            wallet.setBalance(0);
            wallet.setCurrency("USD");
            wallet.setCreatedAt(LocalDateTime.now());
            wallet.setUpdatedAt(LocalDateTime.now());
        }

        // 计算平台服务费，假设按 10% 收取
        int serviceFee = (int) Math.round(amount * 0.10);
        wallet.setBalance(wallet.getBalance() + amount - serviceFee);
        wallet.setUpdatedAt(LocalDateTime.now());
        if (exist) {
            walletMapper.updateById(wallet);
        } else {
            walletMapper.insert(wallet);
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void subtractFunds(Long driverId, Integer amount) {
        Wallet wallet = walletMapper.selectOne(new LambdaQueryWrapper<Wallet>().eq(Wallet::getDriverId, driverId));
        if (wallet == null) {
            throw new RuntimeException("钱包不存在");
        }
        if (wallet.getBalance() < amount) {
            throw new RuntimeException("钱包余额不足");
        }
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletMapper.updateById(wallet);
    }

    @Override
    public WalletDto getWallet(Long driverId) {
        Wallet wallet = walletMapper.selectOne(new LambdaQueryWrapper<Wallet>().eq(Wallet::getDriverId, driverId));
        if (wallet == null) {
            throw new RuntimeException("钱包不存在");
        }
        return new WalletDto(wallet.getDriverId(), wallet.getBalance(), wallet.getUpdatedAt());
    }

    @Override
    public List<Payment> getEarnings(Long driverId, LocalDateTime from, LocalDateTime to) {
        return paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getDriverId, driverId)
                .eq(Payment::getStatus, PaymentStatus.COMPLETED)
                .ge(Payment::getCreatedAt, from)
                .le(Payment::getCreatedAt, to));
    }

    @Override
    public org.springframework.data.domain.Page<WalletTransaction> getWalletTransactions(Long driverId,
            Pageable pageable) {
        Wallet wallet = walletMapper.selectOne(new LambdaQueryWrapper<Wallet>().eq(Wallet::getDriverId, driverId));
        if (wallet == null) {
            throw new ResourceNotFoundException("Wallet not found for driver " + driverId);
        }

        IPage<WalletTransaction> mpPage = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
        mpPage = walletTransactionMapper.selectPage(mpPage, new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getWalletId, wallet.getId()));

        return new PageImpl<>(mpPage.getRecords(), pageable, mpPage.getTotal());
    }

}
