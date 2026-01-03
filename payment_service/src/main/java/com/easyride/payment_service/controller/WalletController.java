package com.easyride.payment_service.controller;

import com.easyride.payment_service.dto.*;
import com.easyride.payment_service.model.Payment;
import com.easyride.payment_service.service.WalletService;
import org.springframework.web.bind.annotation.*;

// @RestController

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.easyride.payment_service.model.WalletTransaction;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    // 获取钱包信息
    @GetMapping("/{driverId}")
    public WalletDto getWallet(@PathVariable Long driverId) {
        return walletService.getWallet(driverId);
    }

    // 获取收入统计
    @GetMapping("/{driverId}/earnings")
    public List<Payment> getEarnings(@PathVariable Long driverId,
            @RequestParam String from,
            @RequestParam String to) {
        LocalDateTime fromDate = LocalDateTime.parse(from);
        LocalDateTime toDate = LocalDateTime.parse(to);
        return walletService.getEarnings(driverId, fromDate, toDate);
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<WalletTransaction>>> getTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Placeholder for driver ID retrieval from userDetails
        Long currentDriverId = 1L; // Should be retrieved from userDetails in real app
        Pageable pageable = PageRequest.of(page, size);
        Page<WalletTransaction> transactions = walletService.getWalletTransactions(currentDriverId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(0, "Success", transactions));
    }
}
