package com.easyride.user_service.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 幂等性服务，防止重复提交
 * 此处采用内存方式存储请求 key，测试时可通过 reset() 方法清空存储
 */
public class IdempotencyService {

    private static final ConcurrentHashMap<String, Boolean> store = new ConcurrentHashMap<>();

    public static boolean isDuplicate(String key) {
        return store.containsKey(key);
    }

    public static void storeKey(String key) {
        store.put(key, Boolean.TRUE);
    }

    // 测试使用：清空存储
    public static void reset() {
        store.clear();
    }
}
