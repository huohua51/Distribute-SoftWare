package com.example.common.dto;

import jakarta.validation.constraints.NotNull;

public record InventoryReserveRequest(
        @NotNull Long userId,
        @NotNull Long productId,
        @NotNull Long orderId
) {
}
