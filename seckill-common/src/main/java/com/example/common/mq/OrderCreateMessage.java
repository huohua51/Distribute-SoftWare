package com.example.common.mq;

public record OrderCreateMessage(
        String messageId,
        Long orderId,
        Long userId,
        Long productId,
        String productName,
        Integer quantity
) {
}
