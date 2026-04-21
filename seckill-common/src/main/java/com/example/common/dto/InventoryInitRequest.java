package com.example.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryInitRequest(
        @NotNull Long productId,
        @Min(1) Integer stock
) {
}
