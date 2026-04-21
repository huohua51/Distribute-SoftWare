package com.example.common.mq;

public record InventoryDeductResultMessage(
        String messageId,
        Long orderId,
        Long userId,
        Long productId,
        boolean success,
        String reason
) {
}
