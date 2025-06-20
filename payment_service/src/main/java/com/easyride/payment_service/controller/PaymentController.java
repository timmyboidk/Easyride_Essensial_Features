package com.easyride.payment_service.controller;

import com.easyride.payment_service.dto.*;
import com.easyride.payment_service.service.PaymentService;
import com.easyride.payment_service.util.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import com.easyride.payment_service.dto.ApiResponse;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/pay")
    public ApiResponse<EncryptedResponseDto> processPayment(@RequestBody EncryptedRequestDto encryptedRequest) {
        try {
            log.info("Received encrypted payment request.");
            String decryptedPayload = EncryptionUtil.decrypt(encryptedRequest.getPayload());
            PaymentRequestDto paymentRequestDto = objectMapper.readValue(decryptedPayload, PaymentRequestDto.class);

            log.info("Processing payment for order ID: {}", paymentRequestDto.getOrderId());
            PaymentResponseDto responseDto = paymentService.processPayment(paymentRequestDto);

            String encryptedResponsePayload = EncryptionUtil.encrypt(objectMapper.writeValueAsString(responseDto));
            log.info("Payment processed successfully for order ID: {}. Sending encrypted response.", paymentRequestDto.getOrderId());
            return ApiResponse.success(new EncryptedResponseDto(encryptedResponsePayload));
        } catch (Exception e) {
            log.error("Payment processing failed", e);
            return ApiResponse.error(500, "支付处理失败: " + e.getMessage());
        }
    }

    @PostMapping("/notify")
    public ApiResponse<Void> handlePaymentNotification(@RequestBody String notificationPayload) {
        log.info("Received payment notification: {}", notificationPayload);
        paymentService.handlePaymentNotification(notificationPayload);
        return ApiResponse.successMessage("Notification received");
    }

    @PostMapping("/refund/{paymentId}")
    public ApiResponse<PaymentResponseDto> refundPayment(@PathVariable String paymentId, @RequestParam(required = false) Integer amount) {
        log.info("Processing refund for payment ID: {}", paymentId);
        PaymentResponseDto responseDto = paymentService.refundPayment(paymentId, amount);
        log.info("Refund processed for payment ID: {}", paymentId);
        return ApiResponse.success("退款处理成功", responseDto);
    }
}