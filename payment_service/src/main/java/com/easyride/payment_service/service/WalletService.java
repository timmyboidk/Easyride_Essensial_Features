package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.WalletDto;
import com.easyride.payment_service.model.Payment;

import java.time.LocalDateTime;
import java.util.List;

public interface WalletService {

    void addFunds(Long orderId, Double amount);

    WalletDto getWallet(Long driverId);

    List<Payment> getEarnings(Long driverId, LocalDateTime from, LocalDateTime to);

    void subtractFunds(Long orderId, Double amount);
}

