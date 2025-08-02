package com.easyride.payment_service.controller;

import com.easyride.payment_service.config.SecurityConfig;
import com.easyride.payment_service.dto.AddPaymentMethodRequestDto;
import com.easyride.payment_service.dto.ApiResponse;
import com.easyride.payment_service.dto.PaymentMethodResponseDto;
import com.easyride.payment_service.interceptor.PaymentSignatureVerificationInterceptor;
import com.easyride.payment_service.model.PaymentMethodType;
import com.easyride.payment_service.service.PassengerPaymentMethodService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentMethodController.class)
@Import(SecurityConfig.class)
@DisplayName("支付方式控制器 (PaymentMethodController) 测试")
class PaymentMethodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PassengerPaymentMethodService paymentMethodService;

    @MockBean
    private PaymentSignatureVerificationInterceptor signatureVerificationInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        when(signatureVerificationInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("POST / - 成功添加一个新的支付方式")
    void addPaymentMethod_shouldSucceed() throws Exception {
        // Given
        Long passengerId = 1L;
        AddPaymentMethodRequestDto request = new AddPaymentMethodRequestDto();
        request.setMethodType(PaymentMethodType.CREDIT_CARD);
        request.setPaymentGatewayNonce("nonce_new");

        PaymentMethodResponseDto serviceResponse = new PaymentMethodResponseDto();
        serviceResponse.setId(101L);
        serviceResponse.setCardBrand("Visa");

        when(paymentMethodService.addPaymentMethod(eq(passengerId), any(AddPaymentMethodRequestDto.class)))
                .thenReturn(serviceResponse);

        // When & Then
        mockMvc.perform(post("/passengers/{passengerId}/payment-methods", passengerId)
                        .with(jwt()) // 假设此端点需要认证
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.id", is(101)));
    }

    @Test
    @DisplayName("GET / - 成功获取一个乘客的所有支付方式")
    void getPaymentMethods_shouldReturnMethods() throws Exception {
        // Given
        Long passengerId = 1L;
        when(paymentMethodService.getPaymentMethods(passengerId)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/passengers/{passengerId}/payment-methods", passengerId)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));
    }

    @Test
    @DisplayName("PUT /{paymentMethodId}/default - 成功设置默认支付方式")
    void setDefaultPaymentMethod_shouldSucceed() throws Exception {
        // Given
        Long passengerId = 1L;
        Long paymentMethodId = 101L;
        // Mock service's void method
        doNothing().when(paymentMethodService).setDefaultPaymentMethod(passengerId, paymentMethodId);

        // When & Then
        mockMvc.perform(put("/passengers/{passengerId}/payment-methods/{paymentMethodId}/default", passengerId, paymentMethodId)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.message", is("默认支付方式设置成功")));
    }

    @Test
    @DisplayName("DELETE /{paymentMethodId} - 成功删除一个支付方式")
    void deletePaymentMethod_shouldSucceed() throws Exception {
        // Given
        Long passengerId = 1L;
        Long paymentMethodId = 101L;
        doNothing().when(paymentMethodService).deletePaymentMethod(passengerId, paymentMethodId);

        // When & Then
        mockMvc.perform(delete("/passengers/{passengerId}/payment-methods/{paymentMethodId}", passengerId, paymentMethodId)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.message", is("支付方式删除成功")));
    }
}