package com.easyride.payment_service.controller;

import com.easyride.payment_service.dto.AddPaymentMethodRequestDto;
import com.easyride.payment_service.dto.PaymentMethodResponseDto;
import com.easyride.payment_service.service.PassengerPaymentMethodService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.easyride.user_service.dto.ApiResponse;

@RestController
@RequestMapping("/passengers/{passengerId}/payment-methods") // Or use @AuthenticationPrincipal
public class PaymentMethodController {

    private static final Logger log = LoggerFactory.getLogger(PaymentMethodController.class);
    private final PassengerPaymentMethodService paymentMethodService;

    @Autowired
    public PaymentMethodController(PassengerPaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @PostMapping
    public com.easyride.user_service.dto.ApiResponse<PaymentMethodResponseDto> addPaymentMethod(
            @PathVariable Long passengerId, // Or get from @AuthenticationPrincipal UserPrincipal principal
            @Valid @RequestBody AddPaymentMethodRequestDto requestDto) {
        // Long actualPassengerId = principal.getId();
        // log.info("Passenger {} attempting to add payment method.", actualPassengerId);
        log.info("Adding payment method for passenger ID: {}", passengerId); // Replace with principal ID logging
        PaymentMethodResponseDto responseDto = paymentMethodService.addPaymentMethod(passengerId, requestDto);
        return ApiResponse.success("支付方式添加成功", responseDto);
    }

    @GetMapping
    public ApiResponse<List<PaymentMethodResponseDto>> getPaymentMethods(
            @PathVariable Long passengerId /* @AuthenticationPrincipal UserPrincipal principal */) {
        // Long actualPassengerId = principal.getId();
        log.info("Fetching payment methods for passenger ID: {}", passengerId);
        List<PaymentMethodResponseDto> methods = paymentMethodService.getPaymentMethods(passengerId);
        return ApiResponse.success(methods);
    }

    @PutMapping("/{paymentMethodId}/default")
    public ApiResponse<String> setDefaultPaymentMethod(
            @PathVariable Long passengerId, /* @AuthenticationPrincipal UserPrincipal principal */
            @PathVariable Long paymentMethodId) {
        // Long actualPassengerId = principal.getId();
        log.info("Setting payment method ID {} as default for passenger {}", paymentMethodId, passengerId);
        paymentMethodService.setDefaultPaymentMethod(passengerId, paymentMethodId);
        return ApiResponse.successMessage("默认支付方式设置成功");
    }

    @DeleteMapping("/{paymentMethodId}")
    public ApiResponse<String> deletePaymentMethod(
            @PathVariable Long passengerId, /* @AuthenticationPrincipal UserPrincipal principal */
            @PathVariable Long paymentMethodId) {
        // Long actualPassengerId = principal.getId();
        log.info("Deleting payment method ID {} for passenger {}", paymentMethodId, passengerId);
        paymentMethodService.deletePaymentMethod(passengerId, paymentMethodId);
        return ApiResponse.successMessage("支付方式删除成功");
    }
}