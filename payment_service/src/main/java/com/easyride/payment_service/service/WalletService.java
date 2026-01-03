package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.WalletDto;
import com.easyride.payment_service.model.Payment;
import com.easyride.payment_service.model.WalletTransaction;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface WalletService {

    void addFunds(Long driverId, Integer amount);

    @Transactional
    void subtractFunds(Long driverId, Integer amount);

    WalletDto getWallet(Long driverId);

    List<Payment> getEarnings(Long driverId, LocalDateTime from, LocalDateTime to);

    Page<WalletTransaction> getWalletTransactions(Long driverId, Pageable pageable);
}
