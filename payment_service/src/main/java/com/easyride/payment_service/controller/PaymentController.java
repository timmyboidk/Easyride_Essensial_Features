package com.easyride.payment_service.controller;

import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;
import com.easyride.payment_service.service.PaymentService;
import com.easyride.payment_service.util.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * 发起支付请求（请求体和响应体均为加密数据）
     */
    @PostMapping("/pay")
    public String processPayment(@Valid @RequestBody String encryptedRequest) {
        try {
            // 解密请求体，获取原始 JSON 字符串
            String decryptedRequest = EncryptionUtil.decrypt(encryptedRequest);
            // 解析 JSON 为 PaymentRequestDto 对象
            PaymentRequestDto paymentRequestDto = objectMapper.readValue(decryptedRequest, PaymentRequestDto.class);

            // 调用支付服务处理支付请求
            PaymentResponseDto responseDto = paymentService.processPayment(paymentRequestDto);

            // 将响应对象转换成 JSON 字符串
            String jsonResponse = objectMapper.writeValueAsString(responseDto);
            // 对响应 JSON 进行加密，并返回加密后的字符串
            return EncryptionUtil.encrypt(jsonResponse);
        } catch (Exception e) {
            throw new RuntimeException("支付处理失败", e);
        }
    }

    /**
     * 接收支付网关的异步通知（此接口保持原样，不加解密）
     */
    @PostMapping("/notify")
    public void handlePaymentNotification(@RequestBody Map<String, String> notificationData) {
        paymentService.handlePaymentNotification(notificationData);
    }

    /**
     * 退款请求（目前退款接口不进行请求体加密，若有需要可类似 processPayment 做加解密）
     */
    @PostMapping("/refund/{paymentId}")
    public void refundPayment(@PathVariable Long paymentId, @RequestParam Integer amount) {
        paymentService.refundPayment(paymentId, amount);
    }
}
