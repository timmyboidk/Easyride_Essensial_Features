package com.easyride.payment_service.strategies;

import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;
import com.easyride.payment_service.model.PassengerPaymentMethod;
import com.easyride.payment_service.model.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
// import com.stripe.Stripe;
// import com.stripe.exception.StripeException;
// import com.stripe.model.PaymentIntent;
// import com.stripe.param.PaymentIntentCreateParams;
// import com.stripe.model.Refund;
// import com.stripe.param.RefundCreateParams;

@Component("STRIPE_CREDIT_CARD") // Bean name matches a value in PaymentMethodType or similar identifier
public class StripeStrategy implements PaymentStrategy {

    private static final Logger log = LoggerFactory.getLogger(StripeStrategy.class);

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    // @PostConstruct // Initialize Stripe SDK
    // public void init() {
    //     Stripe.apiKey = stripeSecretKey;
    // }

    @Override
    public PaymentResponseDto processPayment(PaymentRequestDto paymentRequest, PassengerPaymentMethod paymentMethodDetails) {
        log.info("Processing payment with Stripe for order ID: {}", paymentRequest.getOrderId());
        // Stripe.apiKey = stripeSecretKey; // Ensure API key is set

        if (paymentMethodDetails == null || paymentMethodDetails.getPaymentGatewayToken() == null) {
            log.error("Stripe payment requires a stored payment method token (Stripe PaymentMethod ID or Customer ID + Source).");
            return PaymentResponseDto.builder()
                    .orderId(paymentRequest.getOrderId().toString())
                    .status(PaymentStatus.FAILED)
                    .message("支付方式信息不完整")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // String stripePaymentMethodId = paymentMethodDetails.getPaymentGatewayToken();
        // String stripeCustomerId = paymentMethodDetails.getPaymentGatewayCustomerId(); // Important for stored cards

        try {
            // PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            //     .setAmount((long) (paymentRequest.getAmount() * 100)) // Amount in cents
            //     .setCurrency(paymentRequest.getCurrency().toLowerCase())
            //     .setPaymentMethod(stripePaymentMethodId) // The Stripe PaymentMethod ID
            //     .setCustomer(stripeCustomerId) // Stripe Customer ID
            //     .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.AUTOMATIC) // Or MANUAL for 3DS
            //     .setConfirm(true) // Confirm immediately
            //     // .setOffSession(true) // For recurring or merchant-initiated transactions with saved card
            //     .putMetadata("order_id", paymentRequest.getOrderId().toString())
            //     .build();

            // PaymentIntent paymentIntent = PaymentIntent.create(params);
            log.warn("Stripe PaymentIntent creation logic is a SKELETON. Actual Stripe SDK calls are commented out.");
            // Simulate success for now
            String mockTransactionId = "pi_mock_" + System.currentTimeMillis();

            // switch (paymentIntent.getStatus()) {
            //     case "succeeded":
            //         log.info("Stripe payment successful for order {}: {}", paymentRequest.getOrderId(), paymentIntent.getId());
            //         return PaymentResponseDto.builder()
            //                 .orderId(paymentRequest.getOrderId().toString())
            //                 .status(PaymentStatus.COMPLETED)
            //                 .transactionId(paymentIntent.getId())
            //                 .amount(paymentRequest.getAmount())
            //                 .currency(paymentRequest.getCurrency())
            //                 .message("支付成功")
            //                 .paymentGatewayUsed("STRIPE")
            //                 .timestamp(LocalDateTime.now())
            //                 .build();
            //     case "requires_action":
            //     case "requires_payment_method":
            //     // Handle 3D Secure or other actions
            //        log.info("Stripe payment for order {} requires action: {}", paymentRequest.getOrderId(), paymentIntent.getStatus());
            //        return PaymentResponseDto.builder()...setRequiresAction(true)...setRedirectUrl(...)...build();
            //     default:
            //         log.error("Stripe payment failed for order {}. Status: {}", paymentRequest.getOrderId(), paymentIntent.getStatus());
            //        return PaymentResponseDto.builder().status(PaymentStatus.FAILED)...build();
            // }
            return PaymentResponseDto.builder()
                    .orderId(paymentRequest.getOrderId().toString())
                    .status(PaymentStatus.COMPLETED)
                    .transactionId(mockTransactionId)
                    .amount(paymentRequest.getAmount())
                    .currency(paymentRequest.getCurrency())
                    .message("支付成功 (Stripe Mock)")
                    .paymentGatewayUsed("STRIPE")
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) { // StripeException e
            log.error("Stripe API error during payment for order {}: {}", paymentRequest.getOrderId(), e.getMessage(), e);
            return PaymentResponseDto.builder()
                    .orderId(paymentRequest.getOrderId().toString())
                    .status(PaymentStatus.FAILED)
                    .message("Stripe支付失败: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public PaymentResponseDto refundPayment(String transactionId, Double amount, String currency) {
        log.info("Processing Stripe refund for transaction ID: {}, Amount: {}", transactionId, amount);
        // Stripe.apiKey = stripeSecretKey;
        try {
            // RefundCreateParams params = RefundCreateParams.builder()
            //         .setPaymentIntent(transactionId) // The PaymentIntent ID
            //         .setAmount((long) (amount * 100)) // Amount in cents
            //         .build();
            // Refund refund = Refund.create(params);
            log.warn("Stripe Refund creation logic is a SKELETON. Actual Stripe SDK calls are commented out.");
            // Simulate success
            // if ("succeeded".equals(refund.getStatus())) { ... }
            return PaymentResponseDto.builder()
                    .transactionId(transactionId) // original transaction ID
                    .status(PaymentStatus.REFUNDED) // Or PARTIALLY_REFUNDED
                    .amount(amount)
                    .currency(currency)
                    .message("退款成功 (Stripe Mock)")
                    .paymentGatewayUsed("STRIPE")
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) { // StripeException e
            log.error("Stripe API error during refund for transaction {}: {}", transactionId, e.getMessage(), e);
            return PaymentResponseDto.builder()
                    .transactionId(transactionId)
                    .status(PaymentStatus.REFUND_FAILED)
                    .message("Stripe退款失败: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public boolean supports(String paymentMethodType) {
        // This strategy supports credit/debit cards processed via Stripe.
        // The paymentMethodDetails.methodType would be CREDIT_CARD or DEBIT_CARD,
        // and the system configuration decides Stripe handles these.
        return "CREDIT_CARD".equalsIgnoreCase(paymentMethodType) || "DEBIT_CARD".equalsIgnoreCase(paymentMethodType);
    }
}