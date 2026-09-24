package com.ecommerce.marketplace.inventory.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryResponse(
        UUID id,
        UUID productId,
        String productName,
        int quantity,
        int reservedQuantity,
        int availableQuantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}