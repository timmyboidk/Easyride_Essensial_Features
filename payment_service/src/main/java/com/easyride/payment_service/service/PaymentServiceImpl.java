package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.PaymentEventDto;
import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;
import com.easyride.payment_service.model.Payment;
import com.easyride.payment_service.model.PaymentStatus;
import com.easyride.payment_service.model.TransactionType;
import com.easyride.payment_service.repository.PaymentRepository;
import com.easyride.payment_service.rocketmq.PaymentEventProducer;
import com.easyride.payment_service.util.PaymentGatewayUtil;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final WalletService walletService;
    private final PaymentGatewayUtil paymentGatewayUtil;
    private final PaymentEventProducer paymentEventProducer;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              WalletService walletService,
                              PaymentGatewayUtil paymentGatewayUtil,
                              PaymentEventProducer paymentEventProducer) {
        this.paymentRepository = paymentRepository;
        this.walletService = walletService;
        this.paymentGatewayUtil = paymentGatewayUtil;
        this.paymentEventProducer = paymentEventProducer;
    }

    @Override
    @Transactional
    public PaymentResponseDto processPayment(PaymentRequestDto paymentRequestDto) {
        // 创建支付记录
        Payment payment = new Payment();
        payment.setOrderId(paymentRequestDto.getOrderId());
        payment.setPassengerId(paymentRequestDto.getPassengerId());
        payment.setAmount(paymentRequestDto.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionType(TransactionType.PAYMENT);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 调用支付网关进行支付
        boolean paymentResult = paymentGatewayUtil.processPayment(paymentRequestDto);

        if (paymentResult) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // 更新司机钱包余额
            walletService.addFunds(paymentRequestDto.getOrderId(), paymentRequestDto.getAmount());

            // 发布支付完成事件
            PaymentEventDto paymentEvent = new PaymentEventDto(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus().name(),
                paymentRequestDto.getCurrency(),
                paymentRequestDto.getPaymentMethod()
            );
            paymentEventProducer.sendPaymentEvent(paymentEvent);

            return new PaymentResponseDto(payment.getId(), "COMPLETED", "支付成功");
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            return new PaymentResponseDto(payment.getId(), "FAILED", "支付失败");
        }
    }

    @Override
    public void handlePaymentNotification(Map<String, String> notificationData) {
        // 处理支付网关的异步通知，更新支付状态
    }

    @Override
    @Transactional
    public void refundPayment(Long paymentId, Double amount) {
        // 实现退款逻辑，调用支付网关的退款接口
    }

    @Override
    @Transactional
    public void processOrderPayment(Long orderId) {
        // 根据订单 ID 查找支付记录
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("支付记录不存在"));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new RuntimeException("支付尚未完成");
        }

        // 这里可以执行额外的支付确认逻辑，例如通知订单服务
        // 或者其他需要在支付完成后执行的操作
    }
}


