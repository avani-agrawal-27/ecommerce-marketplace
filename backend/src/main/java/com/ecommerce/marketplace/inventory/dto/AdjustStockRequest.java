package com.ecommerce.marketplace.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdjustStockRequest(

        @Min(value = 1, message = "Quantity must be greater than zero")
        @NotNull(message = "Quantity is required")
        Integer quantity

) {
}