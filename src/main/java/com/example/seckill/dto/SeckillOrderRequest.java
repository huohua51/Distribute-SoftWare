package com.example.seckill.dto;

import jakarta.validation.constraints.NotNull;

public record SeckillOrderRequest(
        @NotNull(message = "用户ID不能为空")
        Long userId,
        @NotNull(message = "商品ID不能为空")
        Long productId
) {
}
