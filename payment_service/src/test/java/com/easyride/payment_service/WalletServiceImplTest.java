package com.easyride.payment_service;

import com.easyride.payment_service.dto.WalletDto;
import com.easyride.payment_service.model.Payment;
import com.easyride.payment_service.model.Wallet;
import com.easyride.payment_service.repository.WalletRepository;
import com.easyride.payment_service.service.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAddFunds_NewWallet() {
        // 假设 orderId 可直接映射为 driverId
        when(walletRepository.findById(100L)).thenReturn(Optional.empty());
        Wallet newWallet = new Wallet();
        newWallet.setDriverId(100L);
        newWallet.setBalance(0);
        newWallet.setUpdatedAt(LocalDateTime.now());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet wallet = invocation.getArgument(0);
            wallet.setDriverId(100L);
            return wallet;
        });

        walletService.addFunds(100L, 10000);
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void testSubtractFunds_Success() {
        Wallet wallet = new Wallet();
        wallet.setDriverId(100L);
        wallet.setBalance(20000);
        wallet.setUpdatedAt(LocalDateTime.now());
        when(walletRepository.findById(100L)).thenReturn(Optional.of(wallet));

        walletService.subtractFunds(100L, 5000);
        assertEquals(15000, wallet.getBalance());
        verify(walletRepository, times(1)).save(wallet);
    }

    @Test
    void testSubtractFunds_InsufficientBalance() {
        Wallet wallet = new Wallet();
        wallet.setDriverId(100L);
        wallet.setBalance(3000);
        when(walletRepository.findById(100L)).thenReturn(Optional.of(wallet));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> walletService.subtractFunds(100L, 5000));
        assertEquals("钱包余额不足", ex.getMessage());
    }

    @Test
    void testGetWallet() {
        Wallet wallet = new Wallet();
        wallet.setDriverId(100L);
        wallet.setBalance(50000);
        wallet.setUpdatedAt(LocalDateTime.now());
        when(walletRepository.findById(100L)).thenReturn(Optional.of(wallet));

        WalletDto dto = walletService.getWallet(100L);
        assertEquals(100L, dto.getDriverId());
        assertEquals(50000, dto.getBalance());
    }

    @Test
    void testGetEarnings() {
        // 测试返回空列表（目前实现返回 new ArrayList<>()）
        List<Payment> earnings = walletService.getEarnings(100L, LocalDateTime.now().minusDays(1), LocalDateTime.now());
        assertNotNull(earnings);
        assertTrue(earnings.isEmpty());
    }
}
