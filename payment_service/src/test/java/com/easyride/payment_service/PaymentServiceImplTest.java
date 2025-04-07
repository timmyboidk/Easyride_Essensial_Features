package com.easyride.payment_service;

import com.easyride.payment_service.dto.PaymentEventDto;
import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;
import com.easyride.payment_service.model.Payment;
import com.easyride.payment_service.model.PaymentStatus;
import com.easyride.payment_service.model.TransactionType;
import com.easyride.payment_service.repository.PaymentRepository;
import com.easyride.payment_service.rocketmq.PaymentEventProducer;
import com.easyride.payment_service.service.PaymentServiceImpl;
import com.easyride.payment_service.service.WalletService;
import com.easyride.payment_service.util.PaymentGatewayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private PaymentGatewayUtil paymentGatewayUtil;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testProcessPayment_DuplicateSubmission() {
        PaymentRequestDto reqDto = new PaymentRequestDto();
        reqDto.setOrderId(100L);
        reqDto.setAmount(10000);
        reqDto.setPaymentMethod("CREDIT_CARD");
        reqDto.setCurrency("USD");
        reqDto.setPassengerId(200L);

        when(redisTemplate.hasKey("payment:100")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> paymentService.processPayment(reqDto));
        assertEquals("重复提交的支付请求", ex.getMessage());
    }

    @Test
    void testProcessPayment_Success() {
        PaymentRequestDto reqDto = new PaymentRequestDto();
        reqDto.setOrderId(100L);
        reqDto.setAmount(10000);
        reqDto.setPaymentMethod("CREDIT_CARD");
        reqDto.setCurrency("USD");
        reqDto.setPassengerId(200L);

        when(redisTemplate.hasKey("payment:100")).thenReturn(false);
        doNothing().when(valueOperations).set(eq("payment:100"), anyString(), eq(60L), eq(TimeUnit.SECONDS));

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrderId(100L);
        payment.setAmount(10000);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionType(TransactionType.PAYMENT);
        payment.setCreatedAt(LocalDateTime.now());
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        when(paymentGatewayUtil.processPayment(reqDto)).thenReturn(true);
        doNothing().when(walletService).addFunds(100L, 10000);

        PaymentResponseDto response = paymentService.processPayment(reqDto);
        assertEquals("COMPLETED", response.getStatus());
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
        verify(paymentEventProducer, atLeastOnce()).sendPaymentEventOrderly(eq("PAYMENT_COMPLETED"), any(PaymentEventDto.class), anyString());
    }

    @Test
    void testProcessPayment_PaymentFailure() {
        PaymentRequestDto reqDto = new PaymentRequestDto();
        reqDto.setOrderId(101L);
        reqDto.setAmount(10000);
        reqDto.setPaymentMethod("CREDIT_CARD");
        reqDto.setCurrency("USD");
        reqDto.setPassengerId(200L);

        when(redisTemplate.hasKey("payment:101")).thenReturn(false);
        doNothing().when(valueOperations).set(eq("payment:101"), anyString(), eq(60L), eq(TimeUnit.SECONDS));

        Payment payment = new Payment();
        payment.setId(2L);
        payment.setOrderId(101L);
        payment.setAmount(10000);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionType(TransactionType.PAYMENT);
        payment.setCreatedAt(LocalDateTime.now());
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        when(paymentGatewayUtil.processPayment(reqDto)).thenReturn(false);

        PaymentResponseDto response = paymentService.processPayment(reqDto);
        assertEquals("FAILED", response.getStatus());
    }

    @Test
    void testRefundPayment_Success() {
        Payment payment = new Payment();
        payment.setId(3L);
        payment.setOrderId(102L);
        payment.setAmount(10000);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCreatedAt(LocalDateTime.now());
        when(paymentRepository.findById(3L)).thenReturn(Optional.of(payment));
        when(paymentGatewayUtil.refundPayment(3L, 5000)).thenReturn(true);
        doNothing().when(walletService).subtractFunds(payment.getOrderId(), 5000);

        assertDoesNotThrow(() -> paymentService.refundPayment(3L, 5000));
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
        verify(paymentEventProducer, atLeastOnce()).sendPaymentEventOrderly(eq("REFUND"), any(), anyString());
    }

    @Test
    void testRefundPayment_Failure_NotCompleted() {
        Payment payment = new Payment();
        payment.setId(4L);
        payment.setOrderId(103L);
        payment.setAmount(10000);
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(4L)).thenReturn(Optional.of(payment));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> paymentService.refundPayment(4L, 5000));
        assertEquals("只能对已完成的支付进行退款", ex.getMessage());
    }
}
