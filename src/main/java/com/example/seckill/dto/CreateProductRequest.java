package com.example.seckill.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateProductRequest(
        Long productId,
        @NotBlank(message = "商品名称不能为空")
        String productName,
        @Min(value = 1, message = "库存必须大于0")
        Integer stock
) {
}
