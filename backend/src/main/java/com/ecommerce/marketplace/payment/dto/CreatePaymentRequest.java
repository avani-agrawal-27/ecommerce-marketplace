package com.ecommerce.marketplace.payment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePaymentRequest(

        @NotNull(message = "Order ID is required")
        UUID orderId

) {
}