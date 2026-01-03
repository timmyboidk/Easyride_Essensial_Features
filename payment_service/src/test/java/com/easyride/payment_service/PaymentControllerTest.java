package com.easyride.payment_service;

import com.easyride.payment_service.controller.PaymentController;
import com.easyride.payment_service.dto.*;
import com.easyride.payment_service.interceptor.PaymentSignatureVerificationInterceptor;
import com.easyride.payment_service.model.PaymentStatus;
import com.easyride.payment_service.service.PaymentService;
import com.easyride.payment_service.util.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private PaymentSignatureVerificationInterceptor signatureInterceptor;

    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        // Pass the interceptor
        when(signatureInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        // Setup simple encryption for testing
        EncryptionUtil.setEncryptionFunction(s -> s);
        EncryptionUtil.setDecryptionFunction(s -> s);
    }

    @Test
    public void testProcessPayment_Success() throws Exception {
        PaymentRequestDto reqDto = new PaymentRequestDto();
        reqDto.setOrderId(100L);
        reqDto.setPassengerId(200L);
        reqDto.setAmount(10000);
        reqDto.setPaymentMethod("CREDIT_CARD");
        reqDto.setCurrency("USD");

        PaymentResponseDto respDto = PaymentResponseDto.builder()
                .paymentId("1")
                .status(PaymentStatus.COMPLETED)
                .message("支付成功")
                .build();
        when(paymentService.processPayment(any(PaymentRequestDto.class))).thenReturn(respDto);

        String requestJson = mapper.writeValueAsString(reqDto);
        EncryptedRequestDto encryptedReq = new EncryptedRequestDto(requestJson);

        mockMvc.perform(post("/payments/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(encryptedReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));
    }

    @Test
    public void testHandlePaymentNotification() throws Exception {
        String notificationJson = "{\"orderId\":\"100\",\"status\":\"COMPLETED\"}";
        mockMvc.perform(post("/payments/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(notificationJson))
                .andExpect(status().isOk());
    }

    @Test
    public void testRefundPayment_Failure() throws Exception {
        when(paymentService.refundPayment(eq("1"), anyInt())).thenThrow(new RuntimeException("退款失败"));

        mockMvc.perform(post("/payments/refund/1")
                .param("amount", "5000"))
                .andExpect(status().isOk()) // Controller catches exception and returns error ApiResponse
                .andExpect(jsonPath("$.code", is(500)));
    }
}
