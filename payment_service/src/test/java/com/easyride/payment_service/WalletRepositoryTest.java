package com.easyride.payment_service;

import com.easyride.payment_service.model.Wallet;
import com.easyride.payment_service.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class WalletRepositoryTest {

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void testWalletCRUD() {
        Wallet wallet = new Wallet();
        wallet.setDriverId(10L);
        wallet.setBalance(50000);
        wallet.setCurrency("USD");
        wallet.setUpdatedAt(LocalDateTime.now());

        walletRepository.save(wallet);
        Optional<Wallet> found = walletRepository.findByDriverId(10L);
        assertThat(found).isPresent();
        assertThat(found.get().getBalance()).isEqualTo(50000);
    }
}
