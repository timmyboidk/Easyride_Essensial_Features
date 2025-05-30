package com.easyride.payment_service;

import com.easyride.payment_service.interceptor.PaymentSignatureVerificationInterceptor;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentSignatureVerificationInterceptorTest {

    private PaymentSignatureVerificationInterceptor interceptor = new PaymentSignatureVerificationInterceptor();
    private static final String SECRET_KEY = "YourSecretKey";

    private String computeSignature(String nonce, String timestamp) throws Exception {
        String data = nonce + timestamp;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hash);
    }

    @Test
    void testPreHandle_MissingHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());
        assertFalse(result);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    }

    @Test
    void testPreHandle_InvalidTimestamp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("nonce", "testNonce");
        // 设置过期的 timestamp（10 分钟前）
        String expiredTimestamp = String.valueOf(Instant.now().minusSeconds(600).getEpochSecond());
        request.addHeader("timestamp", expiredTimestamp);
        String signature = computeSignature("testNonce", expiredTimestamp);
        request.addHeader("signature", signature);

        boolean result = interceptor.preHandle(request, response, new Object());
        assertFalse(result);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    void testPreHandle_InvalidSignature() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String nonce = "testNonce";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        request.addHeader("nonce", nonce);
        request.addHeader("timestamp", timestamp);
        request.addHeader("signature", "invalidSignature");

        boolean result = interceptor.preHandle(request, response, new Object());
        assertFalse(result);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    void testPreHandle_Success() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String nonce = "testNonce";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = computeSignature(nonce, timestamp);
        request.addHeader("nonce", nonce);
        request.addHeader("timestamp", timestamp);
        request.addHeader("signature", signature);

        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
    }
}
