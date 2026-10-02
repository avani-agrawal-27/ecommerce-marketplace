package com.ecommerce.marketplace.payment.dto;

import com.ecommerce.marketplace.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        String paymentReference,
        String provider,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}