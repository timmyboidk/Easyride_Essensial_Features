package com.easyride.payment_service.stress;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;
import com.easyride.payment_service.exception.PaymentServiceException;

import com.easyride.payment_service.model.Payment;
import com.easyride.payment_service.model.PaymentStatus;
import com.easyride.payment_service.repository.PassengerPaymentMethodMapper;
import com.easyride.payment_service.repository.PaymentMapper;
import com.easyride.payment_service.rocketmq.PaymentEventProducer;
import com.easyride.payment_service.service.PaymentServiceImpl;
import com.easyride.payment_service.service.PaymentStrategyProcessor;
import com.easyride.payment_service.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PaymentConcurrencyTest {

    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private WalletService walletService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private PaymentEventProducer paymentEventProducer;
    @Mock
    private PaymentStrategyProcessor strategyProcessor;
    @Mock
    private PassengerPaymentMethodMapper passengerPaymentMethodMapper;

    private PaymentServiceImpl paymentService;

    private final ConcurrentHashMap<String, String> fakeRedis = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentMapper,
                walletService,
                paymentEventProducer,
                redisTemplate,
                null, // gatewayUtil unused
                strategyProcessor,
                passengerPaymentMethodMapper);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // Simulate Redis setIfAbsent (SETNX)
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            String val = inv.getArgument(1);
            return fakeRedis.putIfAbsent(key, val) == null;
        });

        // Mock Strategy Processor
        when(strategyProcessor.processPayment(any(PaymentRequestDto.class))).thenAnswer(inv -> {
            PaymentRequestDto req = inv.getArgument(0);
            return PaymentResponseDto.builder()
                    .transactionId("TXN_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId())
                    .status(PaymentStatus.COMPLETED)
                    .message("Success")
                    .paymentGatewayUsed("STRIPE")
                    .build();
        });

        // Mock Repositories
        when(paymentMapper.insert(any(Payment.class))).thenAnswer(inv -> 1);
        when(passengerPaymentMethodMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);
    }

    @Test
    void testConcurrentPaymentPrevention() throws InterruptedException {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger expectedFailCount = new AtomicInteger(0);

        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(12345L);
        request.setPassengerId(101L);
        request.setAmount(100);
        request.setCurrency("USD");
        request.setPaymentGatewayNonce("fake_nonce");

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    paymentService.processPayment(request);
                    successCount.incrementAndGet();
                } catch (PaymentServiceException e) {
                    if (e.getMessage().contains("正在支付中或已支付")) {
                        expectedFailCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("Payment Success: " + successCount.get());
        System.out.println("Payment Idempotency Fails: " + expectedFailCount.get());

        // Since we simulated Redis correctly, successCount should be EXACTLY 1
        assertEquals(1, successCount.get(), "Only one payment should succeed for the same order");
        assertEquals(threadCount - 1, expectedFailCount.get(), "All other threads should fail with idempotency error");
    }

    @Test
    void testPaymentRegularFlow() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(67890L);
        request.setPassengerId(102L);
        request.setAmount(50);
        request.setCurrency("USD");
        request.setPaymentGatewayNonce("nonce_abc");

        PaymentResponseDto response = paymentService.processPayment(request);

        assertNotNull(response);
        assertEquals(PaymentStatus.COMPLETED, response.getStatus());
        assertTrue(fakeRedis.containsKey("lock:payment:67890"));
    }
}
