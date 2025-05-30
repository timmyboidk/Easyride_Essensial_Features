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
    void testFindByDriverId() {
        Withdrawal w = new Withdrawal();
        w.setDriverId(10L);
        withdrawalRepository.save(w);

        List<Withdrawal> list = withdrawalRepository.findByDriverId(10L);
        assertThat(list).isNotEmpty();
        assertThat(list.get(0).getDriverId()).isEqualTo(10L);
    }
}
