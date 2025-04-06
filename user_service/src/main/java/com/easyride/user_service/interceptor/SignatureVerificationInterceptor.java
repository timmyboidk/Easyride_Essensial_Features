package com.easyride.user_service.interceptor;

import com.easyride.user_service.util.IdempotencyService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Base64;

/**
 * 拦截器用于校验请求签名和幂等性
 * - 检查请求头 nonce、timestamp、signature 是否存在
 * - 校验 timestamp 是否在允许的时间窗口内（防重放攻击）
 * - 利用 HMAC-SHA256 算法计算签名并与传入签名比对
 * - 若请求中存在 Idempotency-Key，则判断是否为重复提交
 */
@Component
public class SignatureVerificationInterceptor implements HandlerInterceptor {

    // 签名计算使用的密钥，实际部署时请妥善管理
    private static final String SECRET_KEY = "YourSecretKey";

    // 允许的时间窗口（单位秒），例如 300 秒即 5 分钟
    private static final long ALLOWED_TIME_WINDOW = 300;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String nonce = request.getHeader("nonce");
        String timestampStr = request.getHeader("timestamp");
        String signature = request.getHeader("signature");
        String idempotencyKey = request.getHeader("Idempotency-Key");

        // 校验必须的请求头是否存在
        if (nonce == null || timestampStr == null || signature == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要的请求头");
            return false;
        }

        // 校验时间戳格式和时间窗口
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
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "签名校验失败");
            return false;
        }

        // 幂等性检查：防止重复提交
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            if (IdempotencyService.isDuplicate(idempotencyKey)) {
                response.sendError(HttpServletResponse.SC_CONFLICT, "检测到重复提交");
                return false;
            }
            IdempotencyService.storeKey(idempotencyKey);
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
