package com.easyride.order_service.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory idempotency service to prevent duplicate submissions.
 *
 * In a production environment, consider using a distributed cache (e.g., Redis)
 * to store idempotency keys with an expiration time.
 */
public class IdempotencyService {

    // In-memory storage for idempotency keys. The key is the idempotency key from the request.
    private static final ConcurrentHashMap<String, Boolean> keyStore = new ConcurrentHashMap<>();

    /**
     * Checks whether the given key has already been used.
     *
     * @param key the idempotency key from the request header.
     * @return true if the key already exists (duplicate submission), false otherwise.
     */
    public static boolean isDuplicate(String key) {
        return keyStore.containsKey(key);
    }

    /**
     * Stores the idempotency key.
     *
     * @param key the idempotency key to store.
     */
    public static void storeKey(String key) {
        keyStore.put(key, Boolean.TRUE);
    }
}
