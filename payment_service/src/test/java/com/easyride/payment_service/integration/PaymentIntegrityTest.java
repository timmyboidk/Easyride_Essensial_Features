package com.easyride.payment_service.integration;

import com.easyride.payment_service.controller.PaymentController;
import com.easyride.payment_service.dto.ApiResponse;
import com.easyride.payment_service.dto.EncryptedRequestDto;
import com.easyride.payment_service.dto.EncryptedResponseDto;
import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.service.PaymentService;
import com.easyride.payment_service.util.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentIntegrityTest {

    @Mock
    private PaymentService paymentService;

    private PaymentController paymentController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        paymentController = new PaymentController(paymentService);
    }

    @Test
    void testNegativeAmount_BlockedByValidation() throws Exception {
        // Goal: Verify if the system BLOCKS negative amounts.

        PaymentRequestDto requestDto = new PaymentRequestDto();
        requestDto.setOrderId(1L);
        requestDto.setAmount(-5000); // Negative amount!
        requestDto.setPaymentMethod("CREDIT_CARD");

        String json = objectMapper.writeValueAsString(requestDto);
        String encrypted = EncryptionUtil.encrypt(json);
        EncryptedRequestDto encryptedRequest = new EncryptedRequestDto(encrypted);

        // When validation fails, controller catches exception and returns error
        // response.
        ApiResponse<EncryptedResponseDto> response = paymentController.processPayment(encryptedRequest);

        // Assert: Error returned
        assertNotNull(response);
        // It returns 500 because it catches generic Exception and wraps it
        assertTrue(response.getCode() != 0);
        assertTrue(response.toString().contains("金额必须大于0") || response.getMessage().contains("金额必须大于0"));

        // Ensure service was NEVER called
        verify(paymentService, never()).processPayment(any());
    }

    @Test
    void testConcurrentPaymentProcessing() throws InterruptedException {
        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        PaymentRequestDto requestDto = new PaymentRequestDto();
        requestDto.setOrderId(2L);
        requestDto.setAmount(100);

        String json;
        try {
            json = objectMapper.writeValueAsString(requestDto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String encrypted = EncryptionUtil.encrypt(json);
        EncryptedRequestDto encryptedRequest = new EncryptedRequestDto(encrypted);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    paymentController.processPayment(encryptedRequest);
                } catch (Exception e) {
                    // ignore
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        verify(paymentService, atLeast(1)).processPayment(any());
    }
}
