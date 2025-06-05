package com.easyride.payment_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "passenger_payment_methods")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassengerPaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long passengerId; // FK to User in User Service

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentMethodType methodType;

    // For card: last4, expiryMonth, expiryYear, cardBrand (e.g., VISA, MASTERCARD)
    // These are display/metadata, actual card details are tokenized by gateway.
    private String cardLastFour;
    private String cardBrand;
    private Integer expiryMonth;
    private Integer expiryYear;

    // Token provided by the payment gateway (Stripe token, PayPal billing agreement ID, etc.)
    // This token is used for actual charges. DO NOT STORE RAW CARD NUMBERS.
    @NotNull
    @Column(unique = true) // A token should be unique for a user's method
    private String paymentGatewayToken;

    private String paymentGatewayCustomerId; // If gateway has customer objects (e.g. Stripe Customer ID)

    private String billingName; // Optional: Name on card/account
    private String billingAddressLine1; // Optional
    private String billingAddressLine2; // Optional
    private String billingCity; // Optional
    private String billingState; // Optional
    private String billingZipCode; // Optional
    private String billingCountry; // Optional

    private boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}