package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.*;
import com.easyride.payment_service.exception.PaymentServiceException;
import com.easyride.payment_service.exception.ResourceNotFoundException;
import com.easyride.payment_service.model.PassengerPaymentMethod;
import com.easyride.payment_service.model.Payment;
import com.easyride.payment_service.model.PaymentStatus;
import com.easyride.payment_service.repository.PassengerPaymentMethodRepository;
import com.easyride.payment_service.repository.PaymentRepository;
import com.easyride.payment_service.rocketmq.PaymentEventProducer;
import com.easyride.payment_service.util.PaymentGatewayUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final WalletService walletService;
    private final PaymentGatewayUtil paymentGatewayUtil;
    private final StringRedisTemplate redisTemplate;
    private final PaymentEventProducer paymentEventProducer;
    private final PaymentStrategyProcessor strategyProcessor; // Inject this
    private final PassengerPaymentMethodRepository passengerPaymentMethodRepository; // Inject this

    // Modify constructor
    public PaymentServiceImpl(PaymentRepository paymentRepository,
            WalletService walletService,
            PaymentEventProducer paymentEventProducer,
            StringRedisTemplate redisTemplate,
            PaymentGatewayUtil paymentGatewayUtil,
            PaymentStrategyProcessor strategyProcessor,
            PassengerPaymentMethodRepository passengerPaymentMethodRepository) {
        this.paymentRepository = paymentRepository;
        this.walletService = walletService;
        this.paymentEventProducer = paymentEventProducer;
        this.redisTemplate = redisTemplate;
        this.paymentGatewayUtil = paymentGatewayUtil;
        this.strategyProcessor = strategyProcessor;
        this.passengerPaymentMethodRepository = passengerPaymentMethodRepository;
    }

    @Override
    @Transactional
    public PaymentResponseDto processPayment(PaymentRequestDto paymentRequestDto) {
        log.info("Processing payment for orderId: {}", paymentRequestDto.getOrderId());

        PassengerPaymentMethod storedPaymentMethod = null;
        if (paymentRequestDto.getPaymentMethodId() != null) {
            storedPaymentMethod = passengerPaymentMethodRepository
                    .findByIdAndPassengerId(paymentRequestDto.getPaymentMethodId(), paymentRequestDto.getPassengerId())
                    .orElseThrow(() -> new PaymentServiceException("选择的支付方式无效或不存在"));
        } else if (paymentRequestDto.getPaymentGatewayNonce() == null) {
            throw new PaymentServiceException("有效的支付方式ID或支付网关nonce必须提供。");
        }

        // Idempotency check
        String lockKey = "lock:payment:" + paymentRequestDto.getOrderId();
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(lockKey, "processing",
                java.time.Duration.ofMinutes(10));
        if (Boolean.FALSE.equals(isNew)) {
            log.warn("Payment for orderId {} is already being processed or completed.", paymentRequestDto.getOrderId());
            throw new PaymentServiceException("该订单正在支付中或已支付。");
        }

        try {
            if (storedPaymentMethod != null) {
                paymentRequestDto.setPaymentMethod(storedPaymentMethod.getMethodType().name());
            }

            PaymentResponseDto strategyResponse = strategyProcessor.processPayment(paymentRequestDto);

            Payment payment = new Payment();
            payment.setOrderId(paymentRequestDto.getOrderId());
            payment.setUserId(paymentRequestDto.getPassengerId());
            payment.setAmount(paymentRequestDto.getAmount());
            payment.setCurrency(paymentRequestDto.getCurrency());
            payment.setStatus(strategyResponse.getStatus());
            payment.setTransactionId(strategyResponse.getTransactionId());
            payment.setPaymentGateway(strategyResponse.getPaymentGatewayUsed());
            payment.setPaymentMethod(paymentRequestDto.getPaymentMethod());
            payment.setCreatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            log.info("Payment record saved with ID {} and status {}", payment.getId(), payment.getStatus());

            if (strategyResponse.getStatus() == PaymentStatus.COMPLETED) {
                try {
                    Long driverId = getDriverIdByOrderId(paymentRequestDto.getOrderId());
                    if (driverId != null) {
                        payment.setDriverId(driverId);
                        paymentRepository.save(payment);
                        walletService.addFunds(driverId, paymentRequestDto.getAmount());
                    } else {
                        log.warn("Could not determine driver ID for order {}. Wallet not updated.",
                                paymentRequestDto.getOrderId());
                    }
                } catch (Exception e) {
                    log.error("Error adding funds to driver's wallet for order {}: {}", paymentRequestDto.getOrderId(),
                            e.getMessage(), e);
                }

                PaymentEventDto event = new PaymentEventDto(payment.getId(), payment.getOrderId(), payment.getUserId(),
                        "PAYMENT_COMPLETED", payment.getStatus().name(), payment.getAmount(), payment.getCurrency(),
                        LocalDateTime.now());
                paymentEventProducer.sendPaymentEvent(event);

            } else if (strategyResponse.getStatus() == PaymentStatus.FAILED) {
                // Clean up lock on failure to allow retry
                redisTemplate.delete(lockKey);
                PaymentFailedEventDto failedEvent = new PaymentFailedEventDto(
                        paymentRequestDto.getOrderId(),
                        paymentRequestDto.getPassengerId(),
                        paymentRequestDto.getAmount(),
                        paymentRequestDto.getCurrency(),
                        strategyResponse.getMessage() != null ? strategyResponse.getMessage() : "支付处理失败",
                        LocalDateTime.now());
                paymentEventProducer.sendPaymentFailedEvent(failedEvent);
            }

            return strategyResponse;
        } catch (Exception e) {
            redisTemplate.delete(lockKey);
            throw e;
        }
    }

    @Transactional
    public void handlePaymentNotification(String notificationPayload) {
        // This method parses the notification and updates payment status accordingly.
        log.info("Received payment notification payload: {}", notificationPayload);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> notificationMap = objectMapper.readValue(notificationPayload, new TypeReference<>() {
            });

            Long OrderId = Long.valueOf(notificationMap.get("OrderId"));
            String status = notificationMap.get("status");

            if (OrderId == null || status == null) {
                log.error("Invalid notification payload: {}", notificationPayload);
                return;
            }

            Payment payment = paymentRepository.findByOrderId(OrderId)
                    .orElseThrow(() -> new RuntimeException("Payment not found for transaction ID: " + OrderId));

            if (payment.getStatus() != PaymentStatus.PENDING) {
                log.warn("Payment {} has already been processed with status: {}", payment.getId(), payment.getStatus());
                return;
            }

            if ("SUCCESS".equalsIgnoreCase(status)) {
                payment.setStatus(PaymentStatus.COMPLETED);
                Long driverId = getDriverIdByOrderId(payment.getOrderId());
                if (driverId != null) {
                    walletService.addFunds(driverId, payment.getAmount());
                } else {
                    log.error("Could not credit wallet for payment {}. Driver ID could not be determined.",
                            payment.getId());
                }
            } else {
                payment.setStatus(PaymentStatus.FAILED);
            }
            paymentRepository.save(payment);
            log.info("Payment {} status updated to {}", payment.getId(), payment.getStatus());

        } catch (IOException e) {
            log.error("Failed to parse payment notification payload.", e);
        }
    }

    @Override
    @Transactional
    public PaymentResponseDto refundPayment(String internalPaymentId, Integer amountToRefund) {
        log.info("Attempting refund for internal payment ID: {}", internalPaymentId);
        Payment payment = paymentRepository.findById(Long.parseLong(internalPaymentId))
                .orElseThrow(() -> new ResourceNotFoundException("支付记录 " + internalPaymentId + " 未找到"));

        if (payment.getStatus() != PaymentStatus.COMPLETED && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new PaymentServiceException("支付状态为 " + payment.getStatus() + ", 无法退款。");
        }

        Integer currentRefundableAmount = payment.getAmount()
                - Optional.ofNullable(payment.getRefundedAmount()).orElse(0);
        Integer refundAmount = (amountToRefund == null || amountToRefund <= 0) ? currentRefundableAmount
                : amountToRefund;

        if (refundAmount > currentRefundableAmount) {
            throw new PaymentServiceException("退款金额超过可退款余额。");
        }

        PaymentResponseDto strategyRefundResponse = strategyProcessor.refundPayment(
                payment.getTransactionId(),
                payment.getPaymentGateway(),
                refundAmount,
                payment.getCurrency());

        if (strategyRefundResponse.getStatus() == PaymentStatus.REFUNDED
                || strategyRefundResponse.getStatus() == PaymentStatus.PARTIALLY_REFUNDED) {
            payment.setRefundedAmount(Optional.ofNullable(payment.getRefundedAmount()).orElse(0) + refundAmount);
            if (payment.getRefundedAmount().equals(payment.getAmount())) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            if (payment.getDriverId() != null) {
                try {
                    walletService.subtractFunds(payment.getDriverId(), refundAmount);
                } catch (Exception e) {
                    log.error("Error subtracting refunded amount from driver {} wallet for payment {}: {}",
                            payment.getDriverId(), payment.getId(), e.getMessage(), e);
                }
            }

            PaymentEventDto event = new PaymentEventDto(payment.getId(), payment.getOrderId(), payment.getUserId(),
                    "PAYMENT_REFUNDED", payment.getStatus().name(), refundAmount, payment.getCurrency(),
                    LocalDateTime.now());
            paymentEventProducer.sendPaymentEvent(event);
            log.info("Refund successful for payment record {}", payment.getId());
        } else {
            log.error("Refund failed at gateway for payment record {}. Message: {}", payment.getId(),
                    strategyRefundResponse.getMessage());
        }
        return strategyRefundResponse;
    }

    private Long getDriverIdByOrderId(Long orderId) {
        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
        if (paymentOpt.isPresent() && paymentOpt.get().getDriverId() != null) {
            return paymentOpt.get().getDriverId();
        }
        // Fallback logic: Without being able to call another service (which would
        // require new models/clients),
        // we cannot retrieve the driver ID if it's not present in the payment record.
        log.warn(
                "Driver ID for order {} not found in the local payment record. Fallback to another service is not possible under current constraints.",
                orderId);
        return null;
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

    @Override
    @Transactional
    public void associateDriverWithOrderPayment(Long orderId, Long driverId) {
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            payment.setDriverId(driverId);
            paymentRepository.save(payment);
            log.info("Associated driver ID {} with payment for order ID {}", driverId, orderId);
        });
    }

}
