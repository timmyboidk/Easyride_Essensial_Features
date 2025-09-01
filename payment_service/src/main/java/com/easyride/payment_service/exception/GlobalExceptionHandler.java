package com.easyride.payment_service.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles specific 'Resource Not Found' exceptions.
     * Returns HTTP 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        String errorMessage = String.format("Resource not found: %s", ex.getMessage());
        log.warn(errorMessage);
        return new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                errorMessage,
                LocalDateTime.now()
        );
    }

    /**
     * Handles custom business logic exceptions within the payment service.
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(PaymentServiceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handlePaymentServiceException(PaymentServiceException ex, WebRequest request) {
        String errorMessage = String.format("Payment service error: %s", ex.getMessage());
        log.error(errorMessage);
        return new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorMessage,
                LocalDateTime.now()
        );
    }

    /**
     * Handles validation exceptions for @Valid on request bodies.
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
        String validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("Field '%s': %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        String errorMessage = String.format("Validation failed. Details: [%s]", validationErrors);
        log.warn(errorMessage);
        return new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorMessage,
                LocalDateTime.now()
        );
    }

    /**
     * Handles validation exceptions for @Validated on method parameters.
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolationException(ConstraintViolationException ex) {
        String violationMessages = ex.getConstraintViolations().stream()
                .map(violation -> String.format("Parameter '%s': %s", violation.getPropertyPath(), violation.getMessage()))
                .collect(Collectors.joining(", "));
        String errorMessage = String.format("Constraint violation. Details: [%s]", violationMessages);
        log.warn(errorMessage);
        return new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorMessage,
                LocalDateTime.now()
        );
    }

    /**
     * Handles exceptions where a request parameter has the wrong type.
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String errorMessage = String.format("Invalid value '%s' provided for parameter '%s'. Expected type: %s.",
                ex.getValue(), ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "Not specified");
        log.warn(errorMessage);
        return new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorMessage,
                LocalDateTime.now()
        );
    }

    /**
     * Handles general, uncaught runtime exceptions as a fallback.
     * Avoids these being caught by the final Exception handler, allowing for a more specific status code if needed.
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleRuntimeException(RuntimeException ex, WebRequest request) {
        // This handler will now catch any RuntimeException that is NOT a subclass of the more specific ones above.
        String errorMessage = String.format("A runtime error occurred: %s", ex.getMessage());
        log.error(errorMessage, ex);
        return new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorMessage,
                LocalDateTime.now()
        );
    }

    /**
     * A final catch-all for any other exceptions not previously handled.
     * Returns HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGlobalException(Exception ex, WebRequest request) {
        String errorMessage = String.format("An unexpected internal server error occurred. Please contact support. Error details: %s", ex.getMessage());
        log.error("Unhandled Exception caught by GlobalExceptionHandler: ", ex);
        return new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected internal server error occurred. Please try again later.", // User-facing message
                LocalDateTime.now()
        );
    }
}