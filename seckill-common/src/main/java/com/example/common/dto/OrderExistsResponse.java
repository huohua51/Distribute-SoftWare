package com.example.common.dto;

public record OrderExistsResponse(
        boolean exists,
        Long orderId,
        String status
) {
}
