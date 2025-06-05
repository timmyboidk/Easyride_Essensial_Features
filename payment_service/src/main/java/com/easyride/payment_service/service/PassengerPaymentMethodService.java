package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.AddPaymentMethodRequestDto;
import com.easyride.payment_service.dto.PaymentMethodResponseDto;
import java.util.List;

public interface PassengerPaymentMethodService {
    PaymentMethodResponseDto addPaymentMethod(Long passengerId, AddPaymentMethodRequestDto requestDto);
    List<PaymentMethodResponseDto> getPaymentMethods(Long passengerId);
    void setDefaultPaymentMethod(Long passengerId, Long paymentMethodId);
    void deletePaymentMethod(Long passengerId, Long paymentMethodId);
}