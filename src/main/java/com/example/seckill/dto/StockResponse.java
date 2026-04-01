package com.example.seckill.dto;

public record StockResponse(
        Long productId,
        String productName,
        Integer dbStock,
        Integer redisStock
) {
}
