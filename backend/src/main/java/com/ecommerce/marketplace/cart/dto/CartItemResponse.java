package com.ecommerce.marketplace.cart.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(

        UUID id,

        UUID productId,

        String productName,

        String sku,

        BigDecimal unitPrice,

        Integer quantity,

        BigDecimal subtotal

) {
}