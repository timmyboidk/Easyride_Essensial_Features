package com.easyride.payment_service;

import com.easyride.payment_service.model.Withdrawal;
import com.easyride.payment_service.repository.WithdrawalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class WithdrawalRepositoryTest {

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    @Test
    void testFindByWalletId() {
        Withdrawal w = new Withdrawal();
        w.setWalletId(10L);
        w.setAmount(100);
        w.setStatus(com.easyride.payment_service.model.WithdrawalStatus.PENDING);
        w.setRequestTime(java.time.LocalDateTime.now());
        withdrawalRepository.save(w);

        List<Withdrawal> list = withdrawalRepository.findByWalletId(10L);
        assertThat(list).isNotEmpty();
        assertThat(list.get(0).getWalletId()).isEqualTo(10L);
    }
}
