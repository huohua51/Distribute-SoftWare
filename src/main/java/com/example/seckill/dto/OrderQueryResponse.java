package com.example.seckill.dto;

import java.time.LocalDateTime;

public record OrderQueryResponse(
        Long orderId,
        Long userId,
        Long productId,
        String productName,
        Integer quantity,
        String status,
        LocalDateTime createdAt
) {
}
