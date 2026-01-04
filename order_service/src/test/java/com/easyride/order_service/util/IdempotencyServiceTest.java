package com.easyride.order_service.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdempotencyServiceTest {

    @Test
    void testIdempotency() {
        String key = "test-key-123";

        // Initially clean
        assertFalse(IdempotencyService.isDuplicate(key));

        // Store key
        IdempotencyService.storeKey(key);

        // Should be duplicate now
        assertTrue(IdempotencyService.isDuplicate(key));
    }
}
