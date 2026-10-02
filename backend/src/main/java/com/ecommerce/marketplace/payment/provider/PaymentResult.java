package com.ecommerce.marketplace.payment.provider;

public record PaymentResult(
        boolean successful,
        String failureReason
) {

    public static PaymentResult success() {
        return new PaymentResult(true, null);
    }

    public static PaymentResult failure(String reason) {
        return new PaymentResult(false, reason);
    }
}