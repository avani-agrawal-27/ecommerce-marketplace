package com.ecommerce.marketplace.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest(

        @NotNull(message = "Category ID is required")
        UUID categoryId,

        @NotBlank(message = "Product name is required")
        @Size(max = 200, message = "Product name must not exceed 200 characters")
        String name,

        @NotBlank(message = "Product slug is required")
        @Size(max = 220, message = "Product slug must not exceed 220 characters")
        String slug,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        String description,

        @NotBlank(message = "SKU is required")
        @Size(max = 100, message = "SKU must not exceed 100 characters")
        String sku,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price must not be negative")
        BigDecimal price
) {
}