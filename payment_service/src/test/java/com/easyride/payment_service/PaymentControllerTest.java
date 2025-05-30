package com.easyride.payment_service;

import com.easyride.payment_service.controller.PaymentController;
import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;
import com.easyride.payment_service.service.PaymentService;
import com.easyride.payment_service.util.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    private ObjectMapper mapper = new ObjectMapper();

    // 为测试时简化，将 EncryptionUtil 的加解密函数设置为原样返回
    @BeforeAll
    public static void setupEncryptionUtil() {
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

        PaymentResponseDto respDto = new PaymentResponseDto(1L, "COMPLETED", "支付成功");
        when(paymentService.processPayment(any(PaymentRequestDto.class))).thenReturn(respDto);

        String requestJson = mapper.writeValueAsString(reqDto);
        String encryptedRequest = EncryptionUtil.encrypt(requestJson);

        mockMvc.perform(post("/payments/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(encryptedRequest)
                        // 模拟签名验证头信息
                        .header("nonce", "testNonce")
                        .header("timestamp", String.valueOf(System.currentTimeMillis()/1000))
                        .header("signature", "testSignature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.message", is("支付成功")));
    }

    @Test
    public void testHandlePaymentNotification() throws Exception {
        // 模拟接收到支付网关的异步通知数据
        String notificationJson = "{\"orderId\":\"100\",\"status\":\"COMPLETED\"}";
        mockMvc.perform(post("/payments/notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificationJson))
                .andExpect(status().isOk());
    }

    @Test
    public void testRefundPayment_Failure() throws Exception {
        doThrow(new RuntimeException("退款失败")).when(paymentService).refundPayment(1L, 5000);
        mockMvc.perform(post("/payments/refund/1")
                        .param("amount", "5000"))
                .andExpect(status().isInternalServerError());
    }
}
