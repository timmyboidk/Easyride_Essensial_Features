package com.easyride.payment_service.strategies;

import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;
import com.easyride.payment_service.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class StripeStrategyTest {

    @InjectMocks
    private StripeStrategy stripeStrategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(stripeStrategy, "stripeApiKey", "sk_test_mock_key");
    }

    @Test
    void supports_Stripe() {
        assertTrue(stripeStrategy.supports("STRIPE"));
    }

    @Test
    void supports_CreditCard() {
        assertTrue(stripeStrategy.supports("CREDIT_CARD"));
    }

    @Test
    void supports_Other_Fail() {
        assertFalse(stripeStrategy.supports("PAYPAL"));
    }

    @Test
    void processPayment_HandlesException() {
        // This test will attempt to call Stripe API with a mock key.
        // It should fail with StripeException (handled) or some other error.
        // We expect it to handle exceptions gracefully returning FAILED status.

        PaymentRequestDto request = new PaymentRequestDto();
        request.setAmount(1000);
        request.setCurrency("USD");
        request.setOrderId(100L);

        // Since we cannot mock static PaymentIntent.create without mockito-inline,
        // we execute the real method. It will likely throw AuthenticationException
        // (StripeException subclass).
        // The catch block in StripeStrategy should catch it and return response.

        PaymentResponseDto response = stripeStrategy.processPayment(request);

        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertTrue(response.getMessage().contains("Stripe Error")
                || response.getMessage().contains("No valid API key provided")
                || response.getMessage().contains("Invalid API Key"));
    }
}
