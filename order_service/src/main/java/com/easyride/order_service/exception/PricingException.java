package com.easyride.order_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for pricing-related errors.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class PricingException extends RuntimeException {

    public PricingException(String message) {
        super(message);
    }

    public PricingException(String message, Throwable cause) {
        super(message, cause);
    }
}
