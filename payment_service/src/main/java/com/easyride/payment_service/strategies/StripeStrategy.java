package com.easyride.payment_service.strategies;

import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.easyride.payment_service.model.PaymentStatus;

import java.math.BigDecimal;

@Slf4j
@Service
public class StripeStrategy implements PaymentStrategy {

    @Value("${stripe.api.key:sk_test_placeholder}")
    private String stripeApiKey;

    @Override
    public PaymentResponseDto processPayment(PaymentRequestDto request) {
        Stripe.apiKey = stripeApiKey;
        try {
            // Stripe expects amount in cents
            Long amountInCents = new BigDecimal(request.getAmount()).multiply(new BigDecimal("100")).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(request.getCurrency().toLowerCase())
                    .setDescription("Ride payment for order: " + request.getOrderId())
                    // In a real app, we would use a payment_method_id from client
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            PaymentResponseDto response = new PaymentResponseDto();
            response.setStatus(PaymentStatus.PENDING); // PaymentIntent needs confirmation
            response.setTransactionId(paymentIntent.getId());
            response.setMessage("Stripe PaymentIntent created. Client secret: " + paymentIntent.getClientSecret());
            // response.setSuccess(true);
            // In a real flow, client confirms payment, and we listen to webhook.
            // For this scope, assuming server creation is success step 1.
            return response;

        } catch (StripeException e) {
            log.error("Stripe payment failed", e);
            PaymentResponseDto response = new PaymentResponseDto();
            // response.setSuccess(false);
            response.setStatus(PaymentStatus.FAILED);
            response.setMessage("Stripe Error: " + e.getMessage());
            return response;
        }
    }

    @Override
    public PaymentResponseDto refundPayment(String transactionId, Integer amount, String currency) {
        Stripe.apiKey = stripeApiKey;
        try {
            // Stripe expects amount in cents
            Long amountInCents = new BigDecimal(amount).multiply(new BigDecimal("100")).longValue();

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(transactionId) // Assuming transactionId is PI ID
                    .setAmount(amountInCents)
                    .build();

            Refund refund = Refund.create(params);

            PaymentResponseDto response = new PaymentResponseDto();
            // response.setSuccess(true);
            response.setTransactionId(refund.getId());
            response.setMessage("Refund successful");
            // Stripe refund status can be 'pending', 'succeeded', 'failed', 'canceled'.
            // Mapping to our enum:
            if ("succeeded".equalsIgnoreCase(refund.getStatus())) {
                response.setStatus(PaymentStatus.COMPLETED);
            } else if ("pending".equalsIgnoreCase(refund.getStatus())) {
                response.setStatus(PaymentStatus.PENDING);
            } else {
                response.setStatus(PaymentStatus.FAILED);
            }
            return response;
        } catch (StripeException e) {
            log.error("Stripe refund failed", e);
            PaymentResponseDto response = new PaymentResponseDto();
            // response.setSuccess(false);
            response.setStatus(PaymentStatus.FAILED);
            response.setMessage("Stripe Refund Error: " + e.getMessage());
            return response;
        }
    }

    @Override
    public boolean supports(String paymentMethodType) {
        return "STRIPE".equalsIgnoreCase(paymentMethodType) || "CREDIT_CARD".equalsIgnoreCase(paymentMethodType);
    }
}