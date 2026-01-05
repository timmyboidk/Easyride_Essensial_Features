package com.easyride.payment_service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easyride.payment_service.model.Wallet;
import com.easyride.payment_service.repository.WalletMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class WalletRepositoryTest {

    @Autowired
    private WalletMapper walletMapper;

    @Test
    void testWalletCRUD() {
        Wallet wallet = new Wallet();
        wallet.setDriverId(10L);
        wallet.setBalance(50000);
        wallet.setCurrency("USD");
        wallet.setUpdatedAt(LocalDateTime.now());

        walletMapper.insert(wallet);
        Wallet found = walletMapper.selectOne(new LambdaQueryWrapper<Wallet>().eq(Wallet::getDriverId, 10L));
        assertThat(found).isNotNull();
        assertThat(found.getBalance()).isEqualTo(50000);
    }
}
