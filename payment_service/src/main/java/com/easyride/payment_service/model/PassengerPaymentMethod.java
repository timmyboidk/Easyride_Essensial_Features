package com.easyride.payment_service.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@TableName("passenger_payment_methods")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassengerPaymentMethod {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long passengerId;

    private PaymentMethodType methodType;

    private String cardLastFour;
    private String cardBrand;
    private Integer expiryMonth;
    private Integer expiryYear;

    private String paymentGatewayToken;

    private String paymentGatewayCustomerId;

    private String billingName;
    private String billingAddressLine1;
    private String billingAddressLine2;
    private String billingCity;
    private String billingState;
    private String billingZipCode;
    private String billingCountry;

    private boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}