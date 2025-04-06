package com.easyride.order_service.interceptor;

import com.easyride.order_service.util.IdempotencyService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Base64;

/**
 * Interceptor to verify the request signature and prevent duplicate submissions.
 *
 * It checks for required headers ("nonce", "timestamp", "signature") and uses a secret key
 * to compute the expected signature. It also verifies that the timestamp is within an allowed
 * window (to prevent replay attacks) and uses an idempotency key (if provided) to block duplicate requests.
 */
@Component
public class SignatureVerificationInterceptor implements HandlerInterceptor {

    // Secret key for HMAC signature computation. In production, secure this properly.
    private static final String SECRET_KEY = "YourSecretKey";

    // Allowed time window in seconds (e.g., 300 seconds = 5 minutes)
    private static final long ALLOWED_TIME_WINDOW = 300;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Retrieve required headers
        String nonce = request.getHeader("nonce");
        String timestampStr = request.getHeader("timestamp");
        String signature = request.getHeader("signature");
        String idempotencyKey = request.getHeader("Idempotency-Key");

        // Verify that required headers are present
        if (nonce == null || timestampStr == null || signature == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required headers");
            return false;
        }

        // Validate timestamp (prevent replay attacks)
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid timestamp format");
            return false;
        }
        long currentTimestamp = Instant.now().getEpochSecond();
        if (Math.abs(currentTimestamp - timestamp) > ALLOWED_TIME_WINDOW) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Request timestamp is outside the allowed window");
            return false;
        }

        // Compute expected signature using HMAC-SHA256
        String data = nonce + timestampStr;
        String expectedSignature = computeHmacSHA256(data, SECRET_KEY);
        if (!expectedSignature.equals(signature)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Signature verification failed");
            return false;
        }

        // Check idempotency to prevent duplicate submissions
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            if (IdempotencyService.isDuplicate(idempotencyKey)) {
                response.sendError(HttpServletResponse.SC_CONFLICT, "Duplicate submission detected");
                return false;
            }
            // Save the idempotency key for later requests.
            // In production, use a distributed cache with an expiration.
            IdempotencyService.storeKey(idempotencyKey);
        }

        // All checks passed; allow the request to proceed
        return true;
    }

    /**
     * Computes the HMAC-SHA256 for the given data using the provided secret key.
     *
     * @param data The data to sign.
     * @param key  The secret key.
     * @return A Base64-encoded signature.
     */
    private String computeHmacSHA256(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
        sha256_HMAC.init(secretKey);
        byte[] hashBytes = sha256_HMAC.doFinal(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hashBytes);
    }
}
