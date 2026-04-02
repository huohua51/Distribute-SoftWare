package com.example.common.mq;

public record PaymentResultMessage(
        String messageId,
        Long paymentId,
        Long orderId,
        Long userId,
        String paymentStatus,
        String reason
) {
}
