package com.easyride.payment_service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easyride.payment_service.model.Withdrawal;
import com.easyride.payment_service.repository.WithdrawalMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class WithdrawalRepositoryTest {

    @Autowired
    private WithdrawalMapper withdrawalMapper;

    @Test
    void testFindByWalletId() {
        Withdrawal w = new Withdrawal();
        w.setWalletId(10L);
        w.setAmount(100);
        w.setStatus(com.easyride.payment_service.model.WithdrawalStatus.PENDING);
        w.setRequestTime(java.time.LocalDateTime.now());
        withdrawalMapper.insert(w);

        List<Withdrawal> list = withdrawalMapper
                .selectList(new LambdaQueryWrapper<Withdrawal>().eq(Withdrawal::getWalletId, 10L));
        assertThat(list).isNotEmpty();
        assertThat(list.get(0).getWalletId()).isEqualTo(10L);
    }
}
