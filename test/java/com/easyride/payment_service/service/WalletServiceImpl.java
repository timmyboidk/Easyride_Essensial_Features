package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.WalletDto;
import com.easyride.payment_service.model.Payment;
import com.easyride.payment_service.model.PaymentStatus;
import com.easyride.payment_service.model.Wallet;
import com.easyride.payment_service.repository.PaymentRepository;
import com.easyride.payment_service.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("钱包服务 (WalletServiceImpl) 测试")
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    @DisplayName("addFunds - 当钱包已存在时，应正确增加余额")
    void addFunds_shouldIncreaseBalance_forExistingWallet() {
        // Given
        Long driverId = 1L;
        Integer initialBalance = 10000;
        Integer amountToAdd = 5000; // 50元

        Wallet existingWallet = new Wallet(driverId, initialBalance, LocalDateTime.now(), 1L);
        Payment payment = new Payment();
        payment.setDriverId(driverId);

        when(paymentRepository.findByOrderId(anyLong())).thenReturn(Optional.of(payment));
        when(walletRepository.findById(driverId)).thenReturn(Optional.of(existingWallet));

        // When
        walletService.addFunds(100L, amountToAdd);

        // Then
        // 平台服务费为 5000 * 0.10 = 500
        Integer expectedBalance = initialBalance + amountToAdd - 500;
        verify(walletRepository).save(argThat(wallet ->
                wallet.getBalance().equals(expectedBalance)
        ));
    }

    @Test
    @DisplayName("addFunds - 当钱包不存在时，应创建新钱包并设置余额")
    void addFunds_shouldCreateWallet_ifNotExists() {
        // Given
        Long driverId = 2L;
        Integer amountToAdd = 8000; // 80元

        Payment payment = new Payment();
        payment.setDriverId(driverId);

        when(paymentRepository.findByOrderId(anyLong())).thenReturn(Optional.of(payment));
        when(walletRepository.findById(driverId)).thenReturn(Optional.empty()); // 钱包不存在

        // When
        walletService.addFunds(101L, amountToAdd);

        // Then
        // 服务费 8000 * 0.1 = 800
        Integer expectedBalance = amountToAdd - 800;
        verify(walletRepository).save(argThat(wallet ->
                wallet.getDriverId().equals(driverId) &&
                        wallet.getBalance().equals(expectedBalance)
        ));
    }

    @Test
    @DisplayName("subtractFunds - 当余额充足时，应正确扣除金额")
    void subtractFunds_shouldDecreaseBalance_whenSufficient() {
        // Given
        Long driverId = 1L;
        Wallet existingWallet = new Wallet(driverId, 10000, LocalDateTime.now(), 1L);
        Payment payment = new Payment();
        payment.setDriverId(driverId);

        when(paymentRepository.findByOrderId(anyLong())).thenReturn(Optional.of(payment));
        when(walletRepository.findById(driverId)).thenReturn(Optional.of(existingWallet));

        // When
        walletService.subtractFunds(102L, 3000);

        // Then
        verify(walletRepository).save(argThat(wallet ->
                wallet.getBalance().equals(7000)
        ));
    }

    @Test
    @DisplayName("subtractFunds - 当余额不足时，应抛出异常")
    void subtractFunds_shouldThrowException_whenInsufficient() {
        // Given
        Long driverId = 1L;
        Wallet existingWallet = new Wallet(driverId, 2000, LocalDateTime.now(), 1L);
        Payment payment = new Payment();
        payment.setDriverId(driverId);

        when(paymentRepository.findByOrderId(anyLong())).thenReturn(Optional.of(payment));
        when(walletRepository.findById(driverId)).thenReturn(Optional.of(existingWallet));

        // When & Then
        assertThatThrownBy(() -> {
            walletService.subtractFunds(103L, 3000);
        })
                .isInstanceOf(RuntimeException.class)
                .hasMessage("钱包余额不足");
    }

    @Test
    @DisplayName("getEarnings - 应只返回指定司机、已完成且在时间范围内的支付记录")
    void getEarnings_shouldFilterCorrectly() {
        // Given
        Long driverId = 7L;
        LocalDateTime from = LocalDateTime.of(2025, 8, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2025, 8, 31, 23, 59);

        Payment correctPayment = new Payment(); // 匹配所有条件
        correctPayment.setDriverId(driverId);
        correctPayment.setStatus(PaymentStatus.COMPLETED);
        correctPayment.setCreatedAt(LocalDateTime.of(2025, 8, 15, 10, 0));

        Payment wrongDriver = new Payment();
        wrongDriver.setDriverId(8L);
        wrongDriver.setStatus(PaymentStatus.COMPLETED);
        wrongDriver.setCreatedAt(LocalDateTime.of(2025, 8, 15, 10, 0));

        Payment wrongStatus = new Payment();
        wrongStatus.setDriverId(driverId);
        wrongStatus.setStatus(PaymentStatus.PENDING);
        wrongStatus.setCreatedAt(LocalDateTime.of(2025, 8, 15, 10, 0));

        Payment outOfTime = new Payment();
        outOfTime.setDriverId(driverId);
        outOfTime.setStatus(PaymentStatus.COMPLETED);
        outOfTime.setCreatedAt(LocalDateTime.of(2025, 7, 15, 10, 0));

        when(paymentRepository.findAll()).thenReturn(List.of(correctPayment, wrongDriver, wrongStatus, outOfTime));

        // When
        List<Payment> earnings = walletService.getEarnings(driverId, from, to);

        // Then
        assertThat(earnings).hasSize(1);
        assertThat(earnings.get(0)).isEqualTo(correctPayment);
    }
}