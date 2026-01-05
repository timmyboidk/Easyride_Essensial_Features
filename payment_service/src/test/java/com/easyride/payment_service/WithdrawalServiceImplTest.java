package com.easyride.payment_service;

import com.easyride.payment_service.dto.WithdrawalRequestDto;
import com.easyride.payment_service.dto.WithdrawalResponseDto;
import com.easyride.payment_service.model.Wallet;
import com.easyride.payment_service.model.Withdrawal;
import com.easyride.payment_service.repository.WalletMapper;
import com.easyride.payment_service.repository.WithdrawalMapper;
import com.easyride.payment_service.service.WithdrawalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class WithdrawalServiceImplTest {

    @Mock
    private WithdrawalMapper withdrawalMapper;

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WithdrawalServiceImpl withdrawalService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRequestWithdrawal_BalanceInsufficient() {
        WithdrawalRequestDto dto = new WithdrawalRequestDto();
        dto.setDriverId(10L);
        dto.setAmount(5000);
        dto.setBankAccount("6222000012345678");
        Wallet wallet = new Wallet();
        wallet.setId(100L);
        wallet.setDriverId(10L);
        wallet.setBalance(3000);
        when(walletMapper.selectOne(any())).thenReturn(wallet);

        WithdrawalResponseDto response = withdrawalService.requestWithdrawal(dto);
        assertEquals("FAILED", response.getStatus());
        assertEquals("余额不足", response.getMessage());
    }

    @Test
    void testRequestWithdrawal_Success() {
        WithdrawalRequestDto dto = new WithdrawalRequestDto();
        dto.setDriverId(10L);
        dto.setAmount(5000);
        dto.setBankAccount("6222000012345678");
        Wallet wallet = new Wallet();
        wallet.setId(100L);
        wallet.setDriverId(10L);
        wallet.setBalance(10000);
        when(walletMapper.selectOne(any())).thenReturn(wallet);
        when(withdrawalMapper.insert(any(Withdrawal.class))).thenReturn(1);

        WithdrawalResponseDto response = withdrawalService.requestWithdrawal(dto);
        assertEquals("PENDING", response.getStatus());
        assertEquals("提现申请已提交", response.getMessage());
    }

    @Test
    void testGetWithdrawalHistory() {
        Wallet wallet = new Wallet();
        wallet.setId(100L);
        wallet.setDriverId(10L);
        when(walletMapper.selectOne(any())).thenReturn(wallet);

        Withdrawal w1 = new Withdrawal();
        w1.setId(1L);
        Withdrawal w2 = new Withdrawal();
        w2.setId(2L);
        List<Withdrawal> history = Arrays.asList(w1, w2);
        when(withdrawalMapper.selectList(any())).thenReturn(history);

        List<Withdrawal> result = withdrawalService.getWithdrawalHistory(10L);
        assertEquals(2, result.size());
    }
}
