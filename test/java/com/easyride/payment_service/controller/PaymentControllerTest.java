package com.easyride.payment_service.controller;

import com.easyride.payment_service.config.SecurityConfig; // 确保导入 SecurityConfig
import com.easyride.payment_service.dto.ApiResponse;
import com.easyride.payment_service.dto.EncryptedRequestDto;
import com.easyride.payment_service.dto.EncryptedResponseDto;
import com.easyride.payment_service.dto.PaymentResponseDto;
import com.easyride.payment_service.exception.ResourceNotFoundException;
import com.easyride.payment_service.interceptor.PaymentSignatureVerificationInterceptor; // 导入拦截器
import com.easyride.payment_service.model.PaymentStatus;
import com.easyride.payment_service.service.PaymentService;
import com.easyride.payment_service.util.EncryptionUtil;
import com.fasterxml.jackson.core.type.TypeReference;
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



import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 1. 明确指定测试目标 Controller
@WebMvcTest(PaymentController.class)
// 2. 导入安全配置，让 @WebMvcTest 知道如何处理认证
@Import(SecurityConfig.class)
@DisplayName("支付控制器 (PaymentController) 测试")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // 3. 使用 @MockBean 模拟 Service 层
    @MockBean
    private PaymentService paymentService;

    // 4. 【关键修复】使用 @MockBean 模拟拦截器
    @MockBean
    private PaymentSignatureVerificationInterceptor signatureVerificationInterceptor;

    private PaymentResponseDto successfulPaymentResponse;

    @BeforeEach
    void setUp() throws Exception { // setUp 也可以抛出异常
        // 关键：我们需要告诉 Mockito，当拦截器的 preHandle 方法被调用时，
        // 直接返回 true，即“放行请求”，不要执行它内部的签名验证逻辑。
        when(signatureVerificationInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        successfulPaymentResponse = new PaymentResponseDto();
        successfulPaymentResponse.setStatus(PaymentStatus.COMPLETED);
        successfulPaymentResponse.setTransactionId("txn_success_123");
        successfulPaymentResponse.setMessage("支付成功");
    }

    @Test
    @DisplayName("POST /pay - 当请求通过认证和签名校验时，应成功处理并返回加密响应")
    void processPayment_shouldReturnEncryptedSuccessResponse_forValidRequest() throws Exception {
        // --- Given ---
        String rawRequestJson = "{\"orderId\":100, \"passengerId\":1, \"amount\":5000}";
        String encryptedRequestPayload = EncryptionUtil.encrypt(rawRequestJson);
        EncryptedRequestDto requestDto = new EncryptedRequestDto();
        requestDto.setPayload(encryptedRequestPayload);

        when(paymentService.processPayment(any())).thenReturn(successfulPaymentResponse);

        // --- When & Then ---
        String responseContent = mockMvc.perform(post("/payments/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        // 模拟认证
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andReturn().getResponse().getContentAsString();

        // --- 深度断言部分保持不变 ---
        TypeReference<ApiResponse<EncryptedResponseDto>> typeRef = new TypeReference<>() {};
        ApiResponse<EncryptedResponseDto> apiResponse = objectMapper.readValue(responseContent, typeRef);

        String decryptedPayload = EncryptionUtil.decrypt(apiResponse.getData().getPayload());
        PaymentResponseDto finalResponse = objectMapper.readValue(decryptedPayload, PaymentResponseDto.class);

        assertThat(finalResponse.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    // 将此方法添加到 PaymentControllerTest 类中

    @Test
    @DisplayName("POST /refund/{paymentId} - 当提供有效支付ID时，应成功处理全额退款")
    void refundPayment_shouldSucceed_forFullRefund() throws Exception {
        // --- Given (安排/准备) ---
        String paymentId = "123";

        // 准备一个由 Service 层成功退款后返回的 DTO
        PaymentResponseDto successfulRefundResponse = new PaymentResponseDto();
        successfulRefundResponse.setStatus(PaymentStatus.REFUNDED);
        successfulRefundResponse.setPaymentId(paymentId);
        successfulRefundResponse.setMessage("退款处理成功");

        // "编排" Mock Service 的行为：
        // 当 paymentService.refundPayment 被以 "123" 和 null (代表全额退款) 调用时，
        // 返回我们预设的 successfulRefundResponse
        when(paymentService.refundPayment(paymentId, null)).thenReturn(successfulRefundResponse);

        // --- When & Then (执行与验证) ---
        mockMvc.perform(post("/payments/refund/{paymentId}", paymentId) // 使用占位符并传入变量
                        .with(jwt())) // 同样，这个端点也可能需要认证

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.message", is("退款处理成功")))
                .andExpect(jsonPath("$.data.status", is("REFUNDED"))) // 验证响应体中 data 字段的内容
                .andExpect(jsonPath("$.data.paymentId", is(paymentId)));
    }

    @Test
    @DisplayName("POST /refund/{paymentId} - 当支付ID不存在时，应返回 404 Not Found")
    void refundPayment_shouldReturnNotFound_whenPaymentIdDoesNotExist() throws Exception {
        // --- Given ---
        String nonExistentPaymentId = "999";

        // 关键：编排 Service 层在被调用时抛出异常
        // 我们使用了更精确的参数匹配器 eq() 和 isNull() 来确保就是这个特定的调用抛出异常
        when(paymentService.refundPayment(eq(nonExistentPaymentId), isNull()))
                .thenThrow(new ResourceNotFoundException("支付记录 " + nonExistentPaymentId + " 未找到"));

        // --- When & Then ---
        mockMvc.perform(post("/payments/refund/{paymentId}", nonExistentPaymentId)
                        .with(jwt())) // 同样，加上认证以保持一致性

                // 关键验证：期望 HTTP 状态码为 404
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /pay - 当 service 层处理失败时，应返回 200 OK 和包含错误信息的响应体")
    void processPayment_shouldReturn200_withErrorBody_whenServiceThrowsException() throws Exception {
        // --- Given (安排/准备) ---
        String rawRequestJson = "{\"orderId\":101, \"passengerId\":2, \"amount\":1000}";
        String encryptedRequestPayload = EncryptionUtil.encrypt(rawRequestJson);
        EncryptedRequestDto requestDto = new EncryptedRequestDto();
        requestDto.setPayload(encryptedRequestPayload);

        String errorMessage = "数据库连接超时";
        when(paymentService.processPayment(any())).thenThrow(new RuntimeException(errorMessage));

        // --- When & Then (执行与验证) ---
        mockMvc.perform(post("/payments/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .with(jwt()))

                // 关键修复：期望 HTTP 状态码为 200 OK，因为异常被 Controller 内部处理了
                .andExpect(status().isOk())

                // 验证响应体的内容是否符合 ApiResponse.error() 的格式
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("支付处理失败: " + errorMessage)))
                .andExpect(jsonPath("$.data").doesNotExist()); // 验证 data 字段为 null 或不存在
    }
}