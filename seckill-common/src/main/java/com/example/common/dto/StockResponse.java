package com.example.common.dto;

public record StockResponse(
        Long productId,
        String productName,
        Integer dbStock,
        Integer redisStock
) {
}
