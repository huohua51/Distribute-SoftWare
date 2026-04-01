package com.example.seckill.support;

public final class RedisKeys {

    private RedisKeys() {
    }

    public static String productStockKey(Long productId) {
        return "seckill:stock:product:" + productId;
    }

    public static String userOrderKey(Long userId, Long productId) {
        return "seckill:order:user:" + userId + ":product:" + productId;
    }

    public static String orderStatusKey(Long orderId) {
        return "seckill:order:status:" + orderId;
    }
}
