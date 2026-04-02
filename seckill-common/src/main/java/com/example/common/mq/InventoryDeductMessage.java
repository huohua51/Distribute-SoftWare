package com.example.common.mq;

public record InventoryDeductMessage(
        String messageId,
        Long orderId,
        Long userId,
        Long productId,
        Integer quantity
) {
}
