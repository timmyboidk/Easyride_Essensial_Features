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
import org.springframework.beans.factory.annotation.Autowired;
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
        // 1. 防止重复提交：利用 Redis 防抖
        String dedupKey = "payment:" + paymentRequestDto.getOrderId();
        Boolean exists = redisTemplate.hasKey(dedupKey);
        if (exists != null && exists) {
            throw new RuntimeException("重复提交的支付请求");
        }
        // 设置防重 key 60 秒过期
        redisTemplate.opsForValue().set(dedupKey, "1", 60, TimeUnit.SECONDS);

        // 2. 创建支付记录（Payment.id 为数据库自增全局唯一标识）
        Payment payment = new Payment();
        payment.setOrderId(paymentRequestDto.getOrderId());
        payment.setPassengerId(paymentRequestDto.getPassengerId());
        // 注意：此处支付金额已采用整型（例如 100 表示 1.00 元或美元）
        payment.setAmount(paymentRequestDto.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionType(TransactionType.PAYMENT);
        payment.setCreatedAt(LocalDateTime.now());
        // 此处建议 Payment 实体中增加 @Version 字段以支持乐观锁
        payment = paymentRepository.save(payment);

        // 3. 生成随机字符串和时间戳，并计算 MD5 签名
        String randomKey = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        String dataToSign = randomKey + timestamp + payment.getId();
        String signature = generateMD5(dataToSign);

        // 将签名及时间戳附加到支付方式字段中，作为网关过滤验证（示例格式，具体根据网关要求调整）
        String securePaymentMethod = paymentRequestDto.getPaymentMethod()
                + "|sig=" + signature
                + "&ts=" + timestamp;
        paymentRequestDto.setPaymentMethod(securePaymentMethod);

        // 4. 调用支付网关，采用异步处理（模拟异步支付，实际调用支付渠道的 SDK）
        boolean paymentResult = paymentGatewayUtil.processPayment(paymentRequestDto);

        if (paymentResult) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // 5. 更新司机钱包余额（扣除平台服务费后增加收入）
            walletService.addFunds(paymentRequestDto.getOrderId(), paymentRequestDto.getAmount());

            // 6. 发布支付完成事件到 RocketMQ，保证同一订单的消息顺序
            PaymentEventDto paymentEvent = new PaymentEventDto(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getAmount(),
                    payment.getStatus().name(),
                    paymentRequestDto.getCurrency(),
                    paymentRequestDto.getPaymentMethod()
            );
            // 采用有序消息发送，使用订单ID作为分区键，并加上 tag "PAYMENT_COMPLETED"
            paymentEventProducer.sendPaymentEventOrderly("PAYMENT_COMPLETED", paymentEvent, payment.getOrderId().toString());

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
        // 解析异步支付网关通知，根据通知数据更新支付状态
        // 此处略（根据实际需求实现）
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
                    amount, // 此处 amount 为整型金额
                    payment.getStatus().name(),
                    "USD", // 假设货币
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
        // 此处可添加额外逻辑，例如通知 order_service 更新订单状态
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
            throw new RuntimeException("生成 MD5 签名失败", e);
        }
    }
}
