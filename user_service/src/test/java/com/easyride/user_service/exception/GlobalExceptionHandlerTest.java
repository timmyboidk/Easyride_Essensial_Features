package com.easyride.user_service.exception;

import com.easyride.user_service.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        webRequest = mock(WebRequest.class);
    }

    @Test
    void handleRuntimeException() {
        RuntimeException ex = new RuntimeException("Runtime error");
        ApiResponse<Object> response = globalExceptionHandler.handleRuntimeException(ex, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getCode());
        assertEquals("Runtime error", response.getMessage());
    }

    @Test
    void handleValidationException() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "Validation failed");
        when(bindingResult.getAllErrors()).thenReturn(Collections.singletonList(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ApiResponse<Object> response = globalExceptionHandler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getCode());
        assertEquals("Validation failed", response.getMessage());
    }

    @Test
    void handleUserAlreadyExistsException() {
        UserAlreadyExistsException ex = new UserAlreadyExistsException("User exists");
        ApiResponse<Object> response = globalExceptionHandler.handleUserAlreadyExistsException(ex);

        assertEquals(HttpStatus.CONFLICT.value(), response.getCode());
        assertEquals("User exists", response.getMessage());
    }

    @Test
    void handleOtpVerificationException() {
        OtpVerificationException ex = new OtpVerificationException("Invalid OTP");
        ApiResponse<Object> response = globalExceptionHandler.handleOtpVerificationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getCode());
        assertEquals("Invalid OTP", response.getMessage());
    }

    @Test
    void handleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
        ApiResponse<Object> response = globalExceptionHandler.handleResourceNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getCode());
        assertEquals("Not found", response.getMessage());
    }

    @Test
    void handleGlobalException() {
        Exception ex = new Exception("Unexpected error");
        ApiResponse<Object> response = globalExceptionHandler.handleGlobalException(ex, webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getCode());
        assertEquals("服务器内部错误，请稍后再试", response.getMessage());
    }
}
