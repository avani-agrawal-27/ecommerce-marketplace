package com.ecommerce.marketplace.order.dto;

import com.ecommerce.marketplace.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID userId,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        String currency,

        String shippingFirstName,
        String shippingLastName,
        String shippingAddressLine1,
        String shippingAddressLine2,
        String shippingCity,
        String shippingState,
        String shippingPostalCode,
        String shippingCountry,
        String shippingPhone,

        List<OrderItemResponse> items,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}