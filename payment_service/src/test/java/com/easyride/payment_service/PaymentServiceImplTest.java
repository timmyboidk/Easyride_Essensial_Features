package com.easyride.payment_service;

import com.easyride.payment_service.dto.*;
import com.easyride.payment_service.exception.PaymentServiceException;
import com.easyride.payment_service.model.*;
import com.easyride.payment_service.repository.PassengerPaymentMethodRepository;
import com.easyride.payment_service.repository.PaymentRepository;
import com.easyride.payment_service.rocketmq.PaymentEventProducer;
import com.easyride.payment_service.service.PaymentServiceImpl;
import com.easyride.payment_service.service.PaymentStrategyProcessor;
import com.easyride.payment_service.service.WalletService;
import com.easyride.payment_service.util.PaymentGatewayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

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

    @Mock
    private PaymentStrategyProcessor strategyProcessor;

    @Mock
    private PassengerPaymentMethodRepository passengerPaymentMethodRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testProcessPayment_Success_WithStoredMethod() {
        PaymentRequestDto reqDto = new PaymentRequestDto();
        reqDto.setOrderId(100L);
        reqDto.setAmount(10000);
        reqDto.setPassengerId(200L);
        reqDto.setPaymentMethodId(1L);
        reqDto.setCurrency("USD");
        reqDto.setPaymentMethod("CREDIT_CARD");

        PassengerPaymentMethod method = new PassengerPaymentMethod();
        method.setId(1L);
        method.setMethodType(PaymentMethodType.CREDIT_CARD);
        when(passengerPaymentMethodRepository.findByIdAndPassengerId(1L, 200L)).thenReturn(Optional.of(method));

        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        PaymentResponseDto strategyResponse = new PaymentResponseDto();
        strategyResponse.setStatus(PaymentStatus.COMPLETED);
        strategyResponse.setTransactionId("TXN123");
        strategyResponse.setPaymentGatewayUsed("STRIPE");
        when(strategyProcessor.processPayment(eq(reqDto))).thenReturn(strategyResponse);

        Payment payment = new Payment();
        payment.setId(1L);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponseDto result = paymentService.processPayment(reqDto);

        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
        verify(paymentEventProducer).sendPaymentEvent(any(PaymentEventDto.class));
    }

    @Test
    void testProcessPayment_IdempotencyFails() {
        PaymentRequestDto reqDto = new PaymentRequestDto();
        reqDto.setOrderId(100L);
        reqDto.setPassengerId(200L);
        reqDto.setPaymentGatewayNonce("nonce");

        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThrows(PaymentServiceException.class, () -> paymentService.processPayment(reqDto));
    }

    @Test
    void testProcessPayment_GatewayFailure() {
        PaymentRequestDto reqDto = new PaymentRequestDto();
        reqDto.setOrderId(101L);
        reqDto.setPassengerId(200L);
        reqDto.setPaymentGatewayNonce("nonce");

        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        PaymentResponseDto strategyResponse = new PaymentResponseDto();
        strategyResponse.setStatus(PaymentStatus.FAILED);
        strategyResponse.setMessage("Insufficient funds");
        when(strategyProcessor.processPayment(eq(reqDto))).thenReturn(strategyResponse);

        PaymentResponseDto result = paymentService.processPayment(reqDto);

        assertEquals(PaymentStatus.FAILED, result.getStatus());
        verify(redisTemplate).delete(contains("101"));
        verify(paymentEventProducer).sendPaymentFailedEvent(any(PaymentFailedEventDto.class));
    }

    @Test
    void testRefundPayment_Success() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrderId(100L);
        payment.setUserId(200L);
        payment.setAmount(10000);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCurrency("USD");
        payment.setTransactionId("TXN123");
        payment.setPaymentGateway("STRIPE");
        payment.setDriverId(500L);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        PaymentResponseDto strategyResponse = new PaymentResponseDto();
        strategyResponse.setStatus(PaymentStatus.REFUNDED);
        when(strategyProcessor.refundPayment(anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(strategyResponse);

        PaymentResponseDto result = paymentService.refundPayment("1", 10000);

        assertEquals(PaymentStatus.REFUNDED, result.getStatus());
        verify(walletService).subtractFunds(500L, 10000);
        verify(paymentRepository).save(payment);
        verify(paymentEventProducer).sendPaymentEvent(any(PaymentEventDto.class));
    }

    @Test
    void testRefundPayment_NotFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> paymentService.refundPayment("99", 1000));
    }
}
