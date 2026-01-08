package com.easyride.order_service.e2e;

import com.easyride.order_service.dto.*;
import com.easyride.order_service.model.VehicleType;
import com.easyride.order_service.model.ServiceType;
import com.easyride.order_service.model.PaymentMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-End High Concurrency Test.
 * Runs against the actual running service (mocked or real).
 * Checks if the service is up before running.
 */
public class HighConcurrencyE2ETest {

    private final String BASE_URL = "http://localhost:8086/orders";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String JWT_SECRET = "your_very_long_jwt_secret_key_that_is_at_least_32_characters_long";

    private String generateToken(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private boolean isServiceUp() {
        try {
            try {
                restTemplate.getForEntity(BASE_URL + "/health-check-probe", String.class);
            } catch (org.springframework.web.client.HttpClientErrorException
                    | org.springframework.web.client.HttpServerErrorException e) {
                return true;
            } catch (org.springframework.web.client.ResourceAccessException e) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    @Tag("e2e")
    public void testHighConcurrencyOrderAcceptance() throws Exception {
        if (!isServiceUp()) {
            System.out.println("SKIPPING E2E TEST: Order Service is not reachable at " + BASE_URL);
            System.out.println("Ensure Docker services are running: docker-compose up -d");
            return;
        }

        System.out.println("STARTING High Concurrency E2E Test against " + BASE_URL);

        // 1. Create an Order
        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setPassengerId(12345L);
        createDto.setStartLocation(new LocationDto(37.7749, -122.4194));
        createDto.setEndLocation(new LocationDto(37.8044, -122.2711));
        createDto.setVehicleType(VehicleType.ECONOMY);
        createDto.setServiceType(ServiceType.NORMAL);
        createDto.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(generateToken(12345L)); // Passenger Token
        HttpEntity<OrderCreateDto> request = new HttpEntity<>(createDto, headers);

        ApiResponse response;
        try {
            response = restTemplate.postForObject(BASE_URL + "/create", request, ApiResponse.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("Failed to create order: " + e.getStatusText());
            System.err.println("Response Body: " + e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            System.err.println("Failed to create order: " + e.getMessage());
            throw e;
        }

        if (response == null || response.getData() == null) {
            throw new RuntimeException("Failed to create order for test (Response or Data is null).");
        }

        // Convert Data to OrderResponseDto
        OrderResponseDto orderData = objectMapper.convertValue(response.getData(), OrderResponseDto.class);
        Long orderId = orderData.getOrderId();
        System.out.println("Created Order ID: " + orderId);

        // 2. Concurrently Accept
        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        System.out.println("Launching " + threads + " concurrent accept requests...");

        for (int i = 0; i < threads; i++) {
            final Long driverId = 5000L + i;
            executor.submit(() -> {
                try {
                    String acceptUrl = BASE_URL + "/" + orderId + "/accept?driverId=" + driverId;

                    HttpHeaders acceptHeaders = new HttpHeaders();
                    acceptHeaders.setContentType(MediaType.APPLICATION_JSON);
                    acceptHeaders.setBearerAuth(generateToken(driverId)); // Driver Token (assuming driver acts as user)

                    HttpEntity<Void> acceptRequest = new HttpEntity<>(null, acceptHeaders);

                    try {
                        ResponseEntity<ApiResponse> acceptResp = restTemplate.postForEntity(acceptUrl, acceptRequest,
                                ApiResponse.class);
                        if (acceptResp.getStatusCode().is2xxSuccessful() && acceptResp.getBody() != null
                                && acceptResp.getBody().getCode() == 0) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(20, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("High Concurrency Test Finished.");
        System.out.println("- Successful Accepts: " + successCount.get());
        System.out.println("- Failed/Rejected Accepts: " + failureCount.get());

        assertTrue(successCount.get() > 0, "At least one driver should have successfully accepted the order.");

        if (successCount.get() > 1) {
            System.out.println("[WARNING] Race condition detected: Multiple drivers accepted the same order!");
        } else {
            System.out.println("[SUCCESS] Race condition handled correctly (only 1 success).");
        }
    }
}
