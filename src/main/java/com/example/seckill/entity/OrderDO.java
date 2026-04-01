package com.example.seckill.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderDO {
    private Long id;
    private Long orderId;
    private Long userId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
