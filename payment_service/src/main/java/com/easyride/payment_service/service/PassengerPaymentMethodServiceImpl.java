package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.AddPaymentMethodRequestDto;
import com.easyride.payment_service.dto.PaymentMethodResponseDto;
import com.easyride.payment_service.exception.ResourceNotFoundException;
import com.easyride.payment_service.exception.PaymentServiceException; // Create this custom exception
import com.easyride.payment_service.model.PassengerPaymentMethod;
import com.easyride.payment_service.model.PaymentMethodType;
import com.easyride.payment_service.repository.PassengerPaymentMethodRepository;
import com.easyride.payment_service.util.PaymentGatewayUtil; // You'll need to enhance this

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PassengerPaymentMethodServiceImpl implements PassengerPaymentMethodService {

    private static final Logger log = LoggerFactory.getLogger(PassengerPaymentMethodServiceImpl.class);

    private final PassengerPaymentMethodRepository paymentMethodRepository;
    private final PaymentGatewayUtil paymentGatewayUtil; // To interact with Stripe/PayPal for token processing

    public PassengerPaymentMethodServiceImpl(PassengerPaymentMethodRepository paymentMethodRepository,
            PaymentGatewayUtil paymentGatewayUtil) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentGatewayUtil = paymentGatewayUtil;
    }

    @Override
    @Transactional
    public PaymentMethodResponseDto addPaymentMethod(Long passengerId, AddPaymentMethodRequestDto requestDto) {
        log.info("Attempting to add payment method for passenger ID {}: {}", passengerId, requestDto.getMethodType());

        // 1. Use the paymentGatewayNonce with the specific payment gateway (Stripe,
        // PayPal)
        // to create a permanent payment method token or customer object.
        // This step is CRITICAL and gateway-specific.
        // For example, with Stripe, you might create a PaymentMethod object and attach
        // it to a Customer.
        // The 'paymentGatewayNonce' is typically a one-time use token.

        PaymentGatewayUtil.GatewayProcessedPaymentMethod processedMethod = paymentGatewayUtil
                .processAndStorePaymentMethodNonce(
                        passengerId,
                        requestDto.getPaymentGatewayNonce(),
                        requestDto.getMethodType());

        // Ensure this nonce/token isn't already stored for this user to prevent
        // duplicates from same nonce.
        if (paymentMethodRepository.findByPaymentGatewayToken(processedMethod.getPermanentToken()).isPresent()) {
            log.warn("Attempt to add duplicate payment method with gateway token {} for passenger {}",
                    processedMethod.getPermanentToken(), passengerId);
            throw new PaymentServiceException("此支付方式似乎已添加。");
        }

        PassengerPaymentMethod newMethod = new PassengerPaymentMethod();
        newMethod.setPassengerId(passengerId);
        newMethod.setMethodType(requestDto.getMethodType());
        newMethod.setPaymentGatewayToken(processedMethod.getPermanentToken()); // Store the PERMANENT token from gateway
        newMethod.setPaymentGatewayCustomerId(processedMethod.getGatewayCustomerId()); // Store gateway customer ID

        newMethod.setCardLastFour(processedMethod.getCardLastFour());
        newMethod.setCardBrand(processedMethod.getCardBrand());
        newMethod.setExpiryMonth(processedMethod.getExpiryMonth());
        newMethod.setExpiryYear(processedMethod.getExpiryYear());
        // newMethod.setBillingName(requestDto.getBillingName()); // If provided

        if (requestDto.isDefault()) {
            // If setting this as default, unset other defaults for this passenger
            paymentMethodRepository.findByPassengerIdAndIsDefaultTrue(passengerId).ifPresent(oldDefault -> {
                oldDefault.setDefault(false);
                paymentMethodRepository.save(oldDefault);
            });
            newMethod.setDefault(true);
        } else {
            // If it's the first payment method, make it default
            if (!paymentMethodRepository.findByPassengerId(passengerId).stream()
                    .anyMatch(PassengerPaymentMethod::isDefault)) {
                newMethod.setDefault(true);
            }
        }

        newMethod.setCreatedAt(LocalDateTime.now());
        PassengerPaymentMethod savedMethod = paymentMethodRepository.save(newMethod);
        log.info("Payment method (ID: {}) added successfully for passenger {}", savedMethod.getId(), passengerId);
        return mapToResponseDto(savedMethod);
    }

    @Override
    public List<PaymentMethodResponseDto> getPaymentMethods(Long passengerId) {
        log.debug("Fetching payment methods for passenger ID {}", passengerId);
        return paymentMethodRepository.findByPassengerId(passengerId).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setDefaultPaymentMethod(Long passengerId, Long paymentMethodId) {
        log.info("Setting payment method ID {} as default for passenger {}", paymentMethodId, passengerId);
        PassengerPaymentMethod newDefault = paymentMethodRepository.findByIdAndPassengerId(paymentMethodId, passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("支付方式未找到或不属于该用户"));

        paymentMethodRepository.findByPassengerIdAndIsDefaultTrue(passengerId).ifPresent(oldDefault -> {
            if (!oldDefault.getId().equals(newDefault.getId())) {
                oldDefault.setDefault(false);
                paymentMethodRepository.save(oldDefault);
            }
        });

        if (!newDefault.isDefault()) {
            newDefault.setDefault(true);
            paymentMethodRepository.save(newDefault);
        }
        log.info("Payment method ID {} is now default for passenger {}", paymentMethodId, passengerId);
    }

    @Override
    @Transactional
    public void deletePaymentMethod(Long passengerId, Long paymentMethodId) {
        log.info("Deleting payment method ID {} for passenger {}", paymentMethodId, passengerId);
        PassengerPaymentMethod methodToDelete = paymentMethodRepository
                .findByIdAndPassengerId(paymentMethodId, passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("支付方式未找到或不属于该用户"));

        // Before deleting from DB, you might need to detach/delete it from the payment
        // gateway's customer object
        paymentGatewayUtil.deleteGatewayPaymentMethod(
                methodToDelete.getPaymentGatewayCustomerId(),
                methodToDelete.getPaymentGatewayToken(),
                methodToDelete.getMethodType());

        paymentMethodRepository.delete(methodToDelete);

        // If the deleted method was default, try to set another one as default
        if (methodToDelete.isDefault()) {
            paymentMethodRepository.findByPassengerId(passengerId).stream().findFirst()
                    .ifPresent(newPotentialDefault -> {
                        newPotentialDefault.setDefault(true);
                        paymentMethodRepository.save(newPotentialDefault);
                        log.info("Set payment method ID {} as new default for passenger {} after deletion.",
                                newPotentialDefault.getId(), passengerId);
                    });
        }
        log.info("Payment method ID {} deleted for passenger {}", paymentMethodId, passengerId);
    }

    private PaymentMethodResponseDto mapToResponseDto(PassengerPaymentMethod method) {
        return PaymentMethodResponseDto.builder()
                .id(method.getId())
                .methodType(method.getMethodType())
                .cardLastFour(method.getCardLastFour())
                .cardBrand(method.getCardBrand())
                .expiryMonth(method.getExpiryMonth())
                .expiryYear(method.getExpiryYear())
                .isDefault(method.isDefault())
                .displayName(buildDisplayName(method))
                .build();
    }

    private String buildDisplayName(PassengerPaymentMethod method) {
        if (method.getMethodType() == PaymentMethodType.CREDIT_CARD
                || method.getMethodType() == PaymentMethodType.DEBIT_CARD) {
            return (method.getCardBrand() != null ? method.getCardBrand() : "Card") + " ending in "
                    + method.getCardLastFour();
        } else if (method.getMethodType() == PaymentMethodType.PAYPAL) {
            return "PayPal"; // Could be PayPal email if gateway provides it safely
        }
        return method.getMethodType().toString();
    }
}