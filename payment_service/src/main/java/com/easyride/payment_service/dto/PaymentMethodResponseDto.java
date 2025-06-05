package com.easyride.payment_service.dto;

import com.easyride.payment_service.model.PaymentMethodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponseDto {
    private Long id;
    private PaymentMethodType methodType;
    private String cardLastFour;
    private String cardBrand;
    private Integer expiryMonth;
    private Integer expiryYear;
    private boolean isDefault;
    private String displayName; // e.g., "Visa ending in 1234"
}