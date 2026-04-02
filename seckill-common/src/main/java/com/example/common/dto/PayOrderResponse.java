package com.example.common.dto;

public record PayOrderResponse(
        Long orderId,
        Long paymentId,
        String paymentStatus
) {
}
