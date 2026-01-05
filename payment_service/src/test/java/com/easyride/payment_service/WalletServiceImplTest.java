package com.easyride.payment_service;

import com.easyride.payment_service.dto.WalletDto;
import com.easyride.payment_service.model.Payment;
import com.easyride.payment_service.model.PaymentStatus;
import com.easyride.payment_service.model.Wallet;
import com.easyride.payment_service.repository.PaymentMapper;
import com.easyride.payment_service.repository.WalletMapper;
import com.easyride.payment_service.repository.WalletTransactionMapper;
import com.easyride.payment_service.service.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WalletServiceImplTest {

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private WalletTransactionMapper walletTransactionMapper;

    @InjectMocks
    private WalletServiceImpl walletService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAddFunds_NewWallet() {
        when(walletMapper.selectOne(any())).thenReturn(null);
        when(walletMapper.insert(any(Wallet.class))).thenReturn(1);

        walletService.addFunds(100L, 10000);

        verify(walletMapper).insert(any(Wallet.class));
    }

    @Test
    void testSubtractFunds_Success() {
        Wallet wallet = new Wallet();
        wallet.setDriverId(100L);
        wallet.setBalance(20000);
        when(walletMapper.selectOne(any())).thenReturn(wallet);
        when(walletMapper.updateById(any(Wallet.class))).thenReturn(1);

        walletService.subtractFunds(100L, 5000);

        assertEquals(15000, wallet.getBalance());
        verify(walletMapper).updateById(wallet);
    }

    @Test
    void testSubtractFunds_InsufficientBalance() {
        Wallet wallet = new Wallet();
        wallet.setDriverId(100L);
        wallet.setBalance(3000);
        when(walletMapper.selectOne(any())).thenReturn(wallet);

        assertThrows(RuntimeException.class, () -> walletService.subtractFunds(100L, 5000));
    }

    @Test
    void testGetWallet() {
        Wallet wallet = new Wallet();
        wallet.setDriverId(100L);
        wallet.setBalance(50000);
        wallet.setUpdatedAt(LocalDateTime.now());
        when(walletMapper.selectOne(any())).thenReturn(wallet);

        WalletDto dto = walletService.getWallet(100L);
        assertEquals(100L, dto.getDriverId());
        assertEquals(50000, dto.getBalance());
    }

    @Test
    void testGetEarnings() {
        LocalDateTime now = LocalDateTime.now();
        Payment p1 = new Payment();
        p1.setDriverId(100L);
        p1.setStatus(PaymentStatus.COMPLETED);
        p1.setCreatedAt(now);

        Payment p2 = new Payment();
        p2.setDriverId(100L);
        p2.setStatus(PaymentStatus.COMPLETED);
        p2.setCreatedAt(now.minusDays(5));

        when(paymentMapper.selectList(any())).thenReturn(Arrays.asList(p1));

        List<Payment> earnings = walletService.getEarnings(100L, now.minusDays(2), now.plusDays(1));

        assertEquals(1, earnings.size());
        assertEquals(p1, earnings.get(0));
    }
}
