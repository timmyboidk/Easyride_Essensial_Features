package com.easyride.payment_service.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;

/**
 * 拦截器用于验证支付请求的签名，防止恶意刷接口和重放攻击。
 * 要求请求头中必须包含：nonce、timestamp 和 signature。
 */
@Component
public class PaymentSignatureVerificationInterceptor implements HandlerInterceptor {

    // 签名密钥，生产环境中建议从配置中心加载
    private static final String SECRET_KEY = "YourSecretKey";

    // 允许的时间窗口（单位秒），例如 300 秒即 5 分钟
    private static final long ALLOWED_TIME_WINDOW = 300;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String nonce = request.getHeader("nonce");
        String timestampStr = request.getHeader("timestamp");
        String signature = request.getHeader("signature");

        if (nonce == null || timestampStr == null || signature == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要的签名验证头信息");
            return false;
        }

        // 校验 timestamp 格式及时效性
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "时间戳格式错误");
            return false;
        }
        long currentTimestamp = Instant.now().getEpochSecond();
        if (Math.abs(currentTimestamp - timestamp) > ALLOWED_TIME_WINDOW) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "请求时间戳超出允许范围");
            return false;
        }

        // 计算预期签名
        String data = nonce + timestampStr;
        String expectedSignature = computeHmacSHA256(data, SECRET_KEY);
        if (!expectedSignature.equals(signature)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "签名验证失败");
            return false;
        }
        return true;
    }

    private String computeHmacSHA256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
        mac.init(secretKey);
        byte[] hashBytes = mac.doFinal(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hashBytes);
    }
}
