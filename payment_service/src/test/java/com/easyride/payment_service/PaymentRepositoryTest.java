package com.easyride.payment_service;

import com.easyride.payment_service.model.Payment;
import com.easyride.payment_service.model.PaymentStatus;
import com.easyride.payment_service.model.TransactionType;
import com.easyride.payment_service.repository.PaymentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class PaymentRepositoryTest {

    @Autowired
    private PaymentMapper paymentMapper;

    @Test
    void testSaveAndFindPayment() {
        Payment payment = new Payment();
        payment.setOrderId(100L);
        payment.setUserId(200L);
        payment.setAmount(10000);
        payment.setCurrency("USD");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionType(TransactionType.PAYMENT);
        payment.setTransactionId("TXN" + System.currentTimeMillis());
        payment.setCreatedAt(LocalDateTime.now());

        paymentMapper.insert(payment);
        Payment found = paymentMapper.selectById(payment.getId());
        assertThat(found).isNotNull();
        assertThat(found.getOrderId()).isEqualTo(100L);
    }
}
