package com.easyride.payment_service.controller;

import com.easyride.payment_service.config.SecurityConfig;
import com.easyride.payment_service.dto.WithdrawalRequestDto;
import com.easyride.payment_service.dto.WithdrawalResponseDto;
import com.easyride.payment_service.interceptor.PaymentSignatureVerificationInterceptor;
import com.easyride.payment_service.model.Withdrawal;
import com.easyride.payment_service.service.WithdrawalService;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WithdrawalController.class)
@Import(SecurityConfig.class)
@DisplayName("提现控制器 (WithdrawalController) 测试")
class WithdrawalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WithdrawalService withdrawalService;

    @MockBean
    private PaymentSignatureVerificationInterceptor signatureVerificationInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        // 同样，我们需要让拦截器放行
        when(signatureVerificationInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("POST /request - 一个有效的提现请求应返回成功响应")
    void requestWithdrawal_shouldReturnSuccessResponse() throws Exception {
        // Given
        WithdrawalRequestDto request = new WithdrawalRequestDto(1L, 5000, "12345");
        WithdrawalResponseDto serviceResponse = new WithdrawalResponseDto(101L, "PENDING", "提现申请已提交");

        when(withdrawalService.requestWithdrawal(any(WithdrawalRequestDto.class))).thenReturn(serviceResponse);

        // When & Then
        mockMvc.perform(post("/withdrawals/request")
                        .with(jwt()) // /withdrawals/request 需要认证
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.withdrawalId", is(101)))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    @DisplayName("GET /{driverId}/history - 应返回指定司机的提现历史")
    void getWithdrawalHistory_shouldReturnHistoryForDriver() throws Exception {
        // Given
        Long driverId = 88L;
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setId(1L);
        withdrawal.setDriverId(driverId);

        when(withdrawalService.getWithdrawalHistory(driverId)).thenReturn(Collections.singletonList(withdrawal));

        // When & Then
        // 注意：/withdrawals/{driverId}/history 是 permitAll，所以可以不加 .with(jwt())
        mockMvc.perform(get("/withdrawals/{driverId}/history", driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // 验证返回的数组大小为1
                .andExpect(jsonPath("$[0].driverId", is(driverId.intValue())));
    }
}