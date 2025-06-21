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
import com.easyride.payment_service.util.EncryptionUtil;
import com.easyride.payment_service.util.PaymentGatewayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                              EncryptionUtil encryptionUtil, // If still used here
                              RedisTemplate<String, String> redisTemplate,
                              PaymentGatewayUtil paymentGatewayUtil, PaymentRepository paymentRepository1, WalletService walletService1, PaymentGatewayUtil paymentGatewayUtil1, StringRedisTemplate redisTemplate1, PaymentEventProducer paymentEventProducer1, // May be less used directly if strategies handle it
                              PaymentStrategyProcessor strategyProcessor, // Added
                              PassengerPaymentMethodRepository passengerPaymentMethodRepository // Added
    ) {
        this.paymentRepository = paymentRepository1;
        this.walletService = walletService1;
        this.paymentGatewayUtil = paymentGatewayUtil1;
        this.redisTemplate = redisTemplate1;
        this.paymentEventProducer = paymentEventProducer1;
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

        PaymentResponseDto strategyResponse = strategyProcessor.processPayment(paymentRequestDto, storedPaymentMethod);

        Payment payment = new Payment();
        payment.setOrderId(paymentRequestDto.getOrderId());
        payment.setPassengerId(paymentRequestDto.getPassengerId());
        payment.setAmount(paymentRequestDto.getAmount());
        payment.setCurrency(paymentRequestDto.getCurrency());
        payment.setStatus(strategyResponse.getStatus());
        payment.setTransactionId(strategyResponse.getTransactionId());
        payment.setPaymentGateway(strategyResponse.getPaymentGatewayUsed());
        payment.setPaymentMethodUsed(paymentRequestDto.getPaymentMethod());
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
                    log.warn("Could not determine driver ID for order {}. Wallet not updated.", paymentRequestDto.getOrderId());
                }
            } catch (Exception e) {
                log.error("Error adding funds to driver's wallet for order {}: {}", paymentRequestDto.getOrderId(), e.getMessage(), e);
            }

            PaymentEventDto event = new PaymentEventDto(payment.getId(), payment.getOrderId(), payment.getPassengerId(),
                    "PAYMENT_COMPLETED", payment.getStatus().name(), payment.getAmount(), payment.getCurrency(), LocalDateTime.now());
            paymentEventProducer.sendPaymentEvent(event);

        } else if (strategyResponse.getStatus() == PaymentStatus.FAILED) {
            PaymentFailedEventDto failedEvent = new PaymentFailedEventDto(
                    paymentRequestDto.getOrderId(),
                    paymentRequestDto.getPassengerId(),
                    paymentRequestDto.getAmount(),
                    paymentRequestDto.getCurrency(),
                    strategyResponse.getMessage() != null ? strategyResponse.getMessage() : "支付处理失败",
                    LocalDateTime.now()
            );
            paymentEventProducer.sendPaymentFailedEvent(failedEvent);
        }

        return strategyResponse;
    }

    @Override
    public void handlePaymentNotification(String notificationPayload) {
        // This method should parse the notification and update payment status accordingly.
        // For example, finding the payment by an ID in the payload and updating its status.
        log.info("Received payment notification payload: {}", notificationPayload);
        // TODO: Implement parsing and handling logic
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

        Integer currentRefundableAmount = payment.getAmount() - Optional.ofNullable(payment.getRefundedAmount()).orElse(0);
        Integer refundAmount = (amountToRefund == null || amountToRefund <= 0) ? currentRefundableAmount : amountToRefund;

        if (refundAmount > currentRefundableAmount) {
            throw new PaymentServiceException("退款金额超过可退款余额。");
        }

        PaymentResponseDto strategyRefundResponse = strategyProcessor.refundPayment(
                payment.getTransactionId(),
                payment.getPaymentGateway(),
                refundAmount,
                payment.getCurrency()
        );

        if (strategyRefundResponse.getStatus() == PaymentStatus.REFUNDED || strategyRefundResponse.getStatus() == PaymentStatus.PARTIALLY_REFUNDED) {
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

            PaymentEventDto event = new PaymentEventDto(payment.getId(), payment.getOrderId(), payment.getPassengerId(),
                    "PAYMENT_REFUNDED", payment.getStatus().name(), refundAmount, payment.getCurrency(), LocalDateTime.now());
            paymentEventProducer.sendPaymentEvent(event);
            log.info("Refund successful for payment record {}", payment.getId());
        } else {
            log.error("Refund failed at gateway for payment record {}. Message: {}", payment.getId(), strategyRefundResponse.getMessage());
        }
        return strategyRefundResponse;
    }


    private Long getDriverIdByOrderId(Long orderId) {
        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
        if (paymentOpt.isPresent() && paymentOpt.get().getDriverId() != null) {
            return paymentOpt.get().getDriverId();
        }
        // TODO: Implement fallback logic to get driver ID, e.g., from another service.
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
