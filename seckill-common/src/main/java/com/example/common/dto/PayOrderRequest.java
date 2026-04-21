package com.example.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PayOrderRequest(
        @NotNull Long orderId,
        @NotNull Long userId,
        @Min(1) Long amountFen
) {
}
