package com.easyride.payment_service.controller;

import com.easyride.payment_service.dto.WalletDto;
import com.easyride.payment_service.model.Payment;
import com.easyride.payment_service.service.WalletService;
import org.springframework.web.bind.annotation.*;

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
}

