package com.ecommerce.marketplace.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(

        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Category slug is required")
        @Size(max = 120, message = "Category slug must not exceed 120 characters")
        String slug,

        @Size(max = 500, message = "Category description must not exceed 500 characters")
        String description

) {
}