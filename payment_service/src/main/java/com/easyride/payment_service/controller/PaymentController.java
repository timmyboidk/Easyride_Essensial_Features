package com.easyride.payment_service.controller;

import com.easyride.payment_service.dto.*;
import com.easyride.payment_service.service.PaymentService;
import com.easyride.payment_service.util.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import com.easyride.payment_service.dto.ApiResponse;


import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EncryptionUtil encryptionUtil

    public PaymentController(PaymentService paymentService, EncryptionUtil encryptionUtil) {
        this.paymentService = paymentService;
        this.encryptionUtil = encryptionUtil;

    }

    /**
     * 发起支付请求（请求体和响应体均为加密数据）
     */
    @PostMapping("/pay")
   public ApiResponse<EncryptedResponseDto> processPayment(@RequestBody EncryptedRequestDto encryptedRequest) {
        try {
            log.info("Received encrypted payment request.");
       String decryptedPayload = encryptionUtil.decrypt(encryptedRequest.getPayload());
        PaymentRequestDto paymentRequestDto = objectMapper.readValue(decryptedPayload, PaymentRequestDto.class); // Assuming objectMapper

        log.info("Processing payment for order ID: {}", paymentRequestDto.getOrderId());
        PaymentResponseDto responseDto = paymentService.processPayment(paymentRequestDto);

        String encryptedResponsePayload = encryptionUtil.encrypt(objectMapper.writeValueAsString(responseDto));
        log.info("Payment processed successfully for order ID: {}. Sending encrypted response.", paymentRequestDto.getOrderId());
        return ApiResponse.success(new EncryptedResponseDto(encryptedResponsePayload));
        } catch (Exception e) {
            throw new RuntimeException("支付处理失败", e);
        }
    }

    /**
     * 接收支付网关的异步通知（此接口保持原样，不加解密）
     */
    @PostMapping("/notify")
    public ApiResponse<Void> handlePaymentNotification(@RequestBody String notificationPayload) {
        log.info("Received payment notification: {}", notificationPayload);
       paymentService.handlePaymentNotification(notificationPayload);
        return ApiResponse.successMessage("Notification received");
    }

    /**
     * 退款请求（目前退款接口不进行请求体加密，若有需要可类似 processPayment 做加解密）
     */
    @PostMapping("/refund/{paymentId}")
    public ApiResponse<PaymentResponseDto> refundPayment(@PathVariable String paymentId, /* @RequestBody RefundRequestDto refundRequest */) {
       log.info("Processing refund for payment ID: {}", paymentId);
       PaymentResponseDto responseDto = paymentService.refundPayment(paymentId /*, refundRequest.getAmount() */);
        log.info("Refund processed for payment ID: {}", paymentId);
        return ApiResponse.success("退款处理成功", responseDto);
    }
}
