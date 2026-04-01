package com.example.seckill.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderTaskDO {
    private Long id;
    private String messageId;
    private Long orderId;
    private Long userId;
    private Long productId;
    private String taskStatus;
    private String failReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
