package com.example.seckill.mq;

public record SeckillOrderMessage(
        String messageId,
        Long orderId,
        Long userId,
        Long productId,
        String productName,
        Integer quantity
) {
}
