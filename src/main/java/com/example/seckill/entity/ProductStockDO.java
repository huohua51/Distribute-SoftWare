package com.example.seckill.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProductStockDO {
    private Long id;
    private Long productId;
    private String productName;
    private Integer totalStock;
    private Integer availableStock;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
