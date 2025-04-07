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
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final WalletService walletService;
    private final PaymentGatewayUtil paymentGatewayUtil;
    private final StringRedisTemplate redisTemplate;
    private final PaymentEventProducer paymentEventProducer;

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              WalletService walletService,
                              PaymentGatewayUtil paymentGatewayUtil,
                              RocketMQTemplate rocketMQTemplate,
                              StringRedisTemplate redisTemplate,
                              PaymentEventProducer paymentEventProducer) {
        this.paymentRepository = paymentRepository;
        this.walletService = walletService;
        this.paymentGatewayUtil = paymentGatewayUtil;
        this.redisTemplate = redisTemplate;
        this.paymentEventProducer = paymentEventProducer;
    }

    @Override
    @Transactional
    public PaymentResponseDto processPayment(PaymentRequestDto paymentRequestDto) {
        // 防重复提交（利用 Redis 防重）
        String dedupKey = "payment:" + paymentRequestDto.getOrderId();
        Boolean exists = redisTemplate.hasKey(dedupKey);
        if (exists != null && exists) {
            logger.warn("重复提交的支付请求，订单ID: {}", paymentRequestDto.getOrderId());
            throw new RuntimeException("重复提交的支付请求");
        }
        redisTemplate.opsForValue().set(dedupKey, "1", 60, TimeUnit.SECONDS);

        // 检查支付金额必须为正整数
        if (paymentRequestDto.getAmount() <= 0) {
            logger.error("支付金额异常，订单ID: {}, 金额: {}", paymentRequestDto.getOrderId(), paymentRequestDto.getAmount());
            throw new RuntimeException("支付金额异常");
        }

        Payment payment = new Payment();
        payment.setOrderId(paymentRequestDto.getOrderId());
        payment.setPassengerId(paymentRequestDto.getPassengerId());
        payment.setAmount(paymentRequestDto.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionType(TransactionType.PAYMENT);
        payment.setCreatedAt(LocalDateTime.now());
        try {
            payment = paymentRepository.save(payment);
        } catch (OptimisticLockingFailureException e) {
            logger.error("支付记录更新出现并发冲突，订单ID: {}", paymentRequestDto.getOrderId(), e);
            throw new RuntimeException("支付记录更新失败，请重试");
        }

        // 计算平台服务费（10%），并更新钱包余额（假设 addFunds 内部已做幂等及并发处理）
        int serviceFee = calculateServiceFee(paymentRequestDto.getAmount());
        try {
            walletService.addFunds(paymentRequestDto.getOrderId(), paymentRequestDto.getAmount());
        } catch (Exception e) {
            logger.error("更新钱包余额失败，订单ID: {}", paymentRequestDto.getOrderId(), e);
            throw new RuntimeException("钱包更新失败");
        }

        // 生成随机字符串、时间戳并计算 MD5 签名（用于支付网关）
        String randomKey = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        String dataToSign = randomKey + timestamp + payment.getId();
        String signature = generateMD5(dataToSign);
        String securePaymentMethod = paymentRequestDto.getPaymentMethod()
                + "|sig=" + signature
                + "&ts=" + timestamp;
        paymentRequestDto.setPaymentMethod(securePaymentMethod);

        boolean paymentResult = paymentGatewayUtil.processPayment(paymentRequestDto);
        if (paymentResult) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setUpdatedAt(LocalDateTime.now());
            try {
                paymentRepository.save(payment);
            } catch (OptimisticLockingFailureException e) {
                logger.error("支付记录更新并发冲突，订单ID: {}", paymentRequestDto.getOrderId(), e);
                throw new RuntimeException("支付记录更新失败，请重试");
            }
            try {
                walletService.addFunds(paymentRequestDto.getOrderId(), paymentRequestDto.getAmount());
            } catch (Exception e) {
                logger.error("更新钱包余额失败（支付成功后），订单ID: {}", paymentRequestDto.getOrderId(), e);
                throw new RuntimeException("钱包更新失败");
            }
            PaymentEventDto paymentEvent = new PaymentEventDto(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getAmount(),
                    payment.getStatus().name(),
                    paymentRequestDto.getCurrency(),
                    paymentRequestDto.getPaymentMethod()
            );
            paymentEventProducer.sendPaymentEventOrderly("PAYMENT_COMPLETED", paymentEvent, payment.getOrderId().toString());
            logger.info("支付成功，订单ID: {}", paymentRequestDto.getOrderId());
            return new PaymentResponseDto(payment.getId(), "COMPLETED", "支付成功");
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            logger.warn("支付失败，订单ID: {}", paymentRequestDto.getOrderId());
            return new PaymentResponseDto(payment.getId(), "FAILED", "支付失败");
        }
    }

    @Override
    public void handlePaymentNotification(Map<String, String> notificationData) {
        // 根据实际需求实现异步通知处理
    }

    @Override
    @Transactional
    public void refundPayment(Long paymentId, Integer amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("支付记录不存在"));
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new RuntimeException("只能对已完成的支付进行退款");
        }
        boolean refundResult = paymentGatewayUtil.refundPayment(paymentId, amount);
        if (refundResult) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            walletService.subtractFunds(payment.getOrderId(), amount);
            PaymentEventDto refundEvent = new PaymentEventDto(
                    payment.getId(),
                    payment.getOrderId(),
                    amount,
                    payment.getStatus().name(),
                    "USD",
                    "REFUND"
            );
            paymentEventProducer.sendPaymentEventOrderly("REFUND", refundEvent, payment.getOrderId().toString());
        } else {
            throw new RuntimeException("退款失败");
        }
    }

    @Override
    @Transactional
    public void processOrderPayment(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("支付记录不存在或未完成"));
        // 此处可添加额外逻辑，例如通知订单服务更新状态
    }

    private int calculateServiceFee(int amount) {
        return (int) Math.round(amount * 0.10);
    }

    private String generateMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("生成 MD5 签名失败", e);
            throw new RuntimeException("生成 MD5 签名失败", e);
        }
    }
}
