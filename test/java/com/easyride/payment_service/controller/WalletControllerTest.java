package com.easyride.payment_service.controller;

import com.easyride.payment_service.config.SecurityConfig;
import com.easyride.payment_service.dto.WalletDto;
import com.easyride.payment_service.interceptor.PaymentSignatureVerificationInterceptor;
import com.easyride.payment_service.model.Payment;
import com.easyride.payment_service.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
@Import(SecurityConfig.class)
@DisplayName("钱包控制器 (WalletController) 测试")
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletService walletService;

    // 即使 WalletController 不在 /payments/** 路径下，为了保持测试环境的一致性，
    // 模拟这个拦截器是良好的实践，以防未来路径配置发生变化。
    @MockBean
    private PaymentSignatureVerificationInterceptor signatureVerificationInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        when(signatureVerificationInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("GET /{driverId} - 应能根据司机ID成功获取钱包信息")
    void getWallet_shouldReturnWalletInfo() throws Exception {
        // Given
        Long driverId = 1L;
        WalletDto walletDto = new WalletDto(driverId, 10000, LocalDateTime.now());
        when(walletService.getWallet(driverId)).thenReturn(walletDto);

        // When & Then
        mockMvc.perform(get("/wallets/{driverId}", driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId", is(driverId.intValue())))
                .andExpect(jsonPath("$.balance", is(10000)));
    }

    @Test
    @DisplayName("GET /{driverId}/earnings - 应能成功获取指定时间范围内的收入列表")
    void getEarnings_shouldReturnEarningsList() throws Exception {
        // Given
        Long driverId = 1L;
        String from = "2025-01-01T00:00:00";
        String to = "2025-01-31T23:59:59";

        Payment earning = new Payment();
        earning.setAmount(5000);
        earning.setDriverId(driverId);
        when(walletService.getEarnings(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(earning));

        // When & Then
        mockMvc.perform(get("/wallets/{driverId}/earnings", driverId)
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].driverId", is(driverId.intValue())))
                .andExpect(jsonPath("$[0].amount", is(5000)));
    }
}