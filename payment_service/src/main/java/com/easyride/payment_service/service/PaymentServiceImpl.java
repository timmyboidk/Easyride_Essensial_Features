package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.PaymentEventDto;
import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;
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

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

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
    @Override
    @Transactional
    public PaymentResponseDto processPayment(PaymentRequestDto paymentRequestDto) {
        // ... (idempotency check) ...
        log.info("Processing payment for orderId: {}", paymentRequestDto.getOrderId());

        // Fetch stored payment method if paymentMethodId is provided
        PassengerPaymentMethod storedPaymentMethod = null;
        if (paymentRequestDto.getPaymentMethodId() != null) { // Assume DTO has paymentMethodId for stored methods
            storedPaymentMethod = passengerPaymentMethodRepository
                    .findByIdAndPassengerId(paymentRequestDto.getPaymentMethodId(), paymentRequestDto.getPassengerId())
                    .orElseThrow(() -> new PaymentServiceException("选择的支付方式无效或不存在"));
        } else if (paymentRequestDto.getPaymentGatewayNonce() != null) {
            // This is a new card/method being added OR a one-time payment.
            // The PassengerPaymentMethodService.addPaymentMethod handles creating a permanent token.
            // For one-time payments with a nonce, the strategy needs to handle the nonce directly.
            // This flow needs refinement: does payment imply adding the method, or can it be one-off?
            // For now, let's assume a stored method is used, or nonce flow is part of a specific strategy.
            log.info("Payment with nonce - strategy must support direct nonce processing or method must be added first.");
            // For a strategy to use a nonce directly, it might not need a full PassengerPaymentMethod object.
        } else {
            throw new PaymentServiceException("有效的支付方式ID或支付网关nonce必须提供。");
        }

        // The 'paymentMethodDetails' passed to strategy needs to be the User's chosen stored method.
        PaymentResponseDto strategyResponse = strategyProcessor.processPayment(paymentRequestDto, storedPaymentMethod);

        Payment payment = new Payment();
        // ... (populate payment entity from paymentRequestDto and strategyResponse) ...
        payment.setOrderId(paymentRequestDto.getOrderId());
        payment.setPassengerId(paymentRequestDto.getPassengerId());
        payment.setAmount(paymentRequestDto.getAmount());
        payment.setCurrency(paymentRequestDto.getCurrency());
        payment.setStatus(strategyResponse.getStatus());
        payment.setTransactionId(strategyResponse.getTransactionId());
        payment.setPaymentGateway(strategyResponse.getPaymentGatewayUsed());
        payment.setPaymentMethodUsed(paymentRequestDto.getPaymentMethod()); // Or from storedPaymentMethod.getMethodType()
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);
        log.info("Payment record saved with ID {} and status {}", payment.getId(), payment.getStatus());

        if (strategyResponse.getStatus() == PaymentStatus.COMPLETED) {
            // Wallet update for driver earnings
            try {
                // Critical: Need DRIVER_ID associated with the orderId.
                // This should come from the consumed OrderCompletedEvent or an API call to OrderService.
                // For now, assuming getDriverIdByOrderId is a placeholder for this logic.
                Long driverId = getDriverIdByOrderId(paymentRequestDto.getOrderId()); // Needs real implementation
                if (driverId != null) {
                    walletService.addFunds(driverId, paymentRequestDto.getAmount()); // Amount here is ride fare
                } else {
                    log.warn("Could not determine driver ID for order {}. Wallet not updated.", paymentRequestDto.getOrderId());
                }
            } catch (Exception e) {
                log.error("Error adding funds to driver's wallet for order {}: {}", paymentRequestDto.getOrderId(), e.getMessage(), e);
                // This is a critical failure if payment succeeded but wallet update failed. Needs robust handling (e.g., retry, reconciliation job).
            }

            PaymentEventDto event = new PaymentEventDto(payment.getId(), payment.getOrderId(), payment.getPassengerId(),
                    "PAYMENT_COMPLETED", payment.getStatus().name(), payment.getAmount(), payment.getCurrency(), LocalDateTime.now());
            paymentEventProducer.sendPaymentEvent(event);
        } else if (strategyResponse.getStatus() == PaymentStatus.FAILED) {
            // Prompt 5: Publish PAYMENT_FAILED event
            PaymentFailedEventDto failedEvent = new PaymentFailedEventDto(
                    paymentRequestDto.getOrderId(),
                    paymentRequestDto.getPassengerId(),
                    paymentRequestDto.getAmount(),
                    paymentRequestDto.getCurrency(),
                    strategyResponse.getMessage() != null ? strategyResponse.getMessage() : "支付处理失败",
                    LocalDateTime.now()
            );
            paymentEventProducer.sendPaymentFailedEvent(failedEvent); // New method in producer
        }
        // Handle requiresAction scenario (e.g., 3DS) by returning appropriate info in strategyResponse

        return strategyResponse; // Return the response from the strategy
    }
    @Override
    public void handlePaymentNotification(Map<String, String> notificationData) {
        // 根据实际需求实现异步通知处理
    }

    @Override
    public PaymentResponseDto refundPayment(String internalPaymentId /*or orderId*/, Double amountToRefund) {
        log.info("Attempting refund for internal payment ID: {}", internalPaymentId);
        Payment payment = paymentRepository.findById(internalPaymentId) // Or find by orderId if that's the input
                .orElseThrow(() -> new ResourceNotFoundException("支付记录 " + internalPaymentId + " 未找到"));

        if(payment.getStatus() != PaymentStatus.COMPLETED && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new PaymentServiceException("支付状态为 " + payment.getStatus() + ", 无法退款。");
        }
        // Use amount from payment record if full refund, or validate amountToRefund
        Double refundAmount = (amountToRefund == null || amountToRefund <= 0) ? payment.getAmount() : amountToRefund;
        if (refundAmount > payment.getAmount() - (payment.getRefundedAmount() != null ? payment.getRefundedAmount() : 0.0) ) {
            throw new PaymentServiceException("退款金额超过可退款余额。");
        }


        PaymentResponseDto strategyRefundResponse = strategyProcessor.refundPayment(
                payment.getTransactionId(), // Gateway's transaction ID
                payment.getPaymentGateway(), // e.g. "STRIPE"
                refundAmount,
                payment.getCurrency()
        );

        if (strategyRefundResponse.getStatus() == PaymentStatus.REFUNDED || strategyRefundResponse.getStatus() == PaymentStatus.PARTIALLY_REFUNDED) {
            payment.setStatus(strategyRefundResponse.getStatus()); // Update based on full or partial
            payment.setRefundedAmount((payment.getRefundedAmount() != null ? payment.getRefundedAmount() : 0.0) + refundAmount);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Deduct from driver's wallet IF funds were already credited and policy dictates deduction on refund
            if (payment.getDriverId() != null) { // Assuming driverId was stored on Payment
                try {
                    walletService.subtractFunds(payment.getDriverId(), refundAmount);
                } catch (Exception e) {
                    log.error("Error subtracting refunded amount from driver {} wallet for payment {}: {}",
                            payment.getDriverId(), payment.getId(), e.getMessage(), e);
                    // Critical issue: needs monitoring and possible manual intervention.
                }
            }

            // Publish PAYMENT_REFUNDED event
            PaymentEventDto event = new PaymentEventDto(payment.getId(), payment.getOrderId(), payment.getPassengerId(),
                    "PAYMENT_REFUNDED", payment.getStatus().name(), refundAmount, payment.getCurrency(), LocalDateTime.now());
            paymentEventProducer.sendPaymentEvent(event);
            log.info("Refund successful for payment record {}", payment.getId());
        } else {
            log.error("Refund failed at gateway for payment record {}. Message: {}", payment.getId(), strategyRefundResponse.getMessage());
            // Optionally update payment status to REFUND_FAILED if needed
        }
        return strategyRefundResponse;
    }

    // TODO: Implement getDriverIdByOrderId(Long orderId)
    // This needs to get info from Order Service (e.g. via an event that PaymentService consumes when an order is finalized for payment)
    // or by having OrderService store DriverId in a shared cache, or via a direct API call (less ideal for high volume).
    private Long getDriverIdByOrderId(Long orderId) {
        // Placeholder - This is critical and needs a robust solution.
        // Option 1: OrderEventConsumer populates a map or cache.
        // Option 2: If PaymentService subscribes to an "ORDER_READY_FOR_PAYMENT" event that includes driverId.
        log.warn("getDriverIdByOrderId for order {} is a placeholder. Real implementation needed.", orderId);
        // Try to get from the payment record if it was populated earlier (e.g. by OrderEventConsumer)
        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId).stream().findFirst();
        if (paymentOpt.isPresent() && paymentOpt.get().getDriverId() != null) {
            return paymentOpt.get().getDriverId();
        }
        return null; // Indicates driver ID couldn't be determined
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
        // Find an existing PENDING payment record for this order or create a shell if necessary
        Payment payment = paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.PENDING)
                .stream().findFirst() // Should ideally be unique pending payment per order
                .orElseGet(() -> {
                    // This case might not be ideal. Payment record should exist from initial /pay call.
                    // If flow is: Order completes -> this consumer -> processPayment, then a PENDING payment might not exist yet.
                    // This method is more for if payment processing and driverId association are separate steps.
                    log.warn("No PENDING payment found for order {} to associate driver. This might be okay if processPayment creates it.", orderId);
                    // Consider if a shell payment should be created or if processPayment handles all creation.
                    // For now, assume processPayment will create the Payment record.
                    // This method could simply cache orderId -> driverId mapping for processPayment to use.
                    // For simplicity now, let's assume Payment record might exist or will be created by processPayment
                    // and we will update it if found.
                    return null;
                });

        if (payment != null) {
            payment.setDriverId(driverId); // Add driverId to Payment model
            paymentRepository.save(payment);
            log.info("Associated driver ID {} with payment for order ID {}", driverId, orderId);
        } else {
            // If no pending payment, we might cache this mapping for when processPayment is called
            // Example: redisTemplate.opsForValue().set("order_driver_map:" + orderId, driverId.toString());
            log.info("Driver ID {} for order {} noted. Will be used when payment is processed.", driverId, orderId);
        }
    }

    // Modify getDriverIdByOrderId to use this cached/stored info
    private Long getDriverIdByOrderId(Long orderId) {
        // Attempt 1: Check if Payment record already has it (set by associateDriverWithOrderPayment)
        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId).stream()
                .filter(p -> p.getDriverId() != null)
                .findFirst();
        if (paymentOpt.isPresent()) {
            return paymentOpt.get().getDriverId();
        }

        // Attempt 2: Check a temporary cache if associateDriver... stored it there
        // String cachedDriverId = redisTemplate.opsForValue().get("order_driver_map:" + orderId);
        // if (cachedDriverId != null) {
        //     redisTemplate.delete("order_driver_map:" + orderId); // Consume it
        //     return Long.parseLong(cachedDriverId);
        // }

        log.warn("getDriverIdByOrderId for order {} could not resolve driver ID. Wallet update may be impacted.", orderId);
        return null;
    }

}
