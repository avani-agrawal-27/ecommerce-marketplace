package com.ecommerce.marketplace.payment.provider;

import com.ecommerce.marketplace.payment.entity.Payment;

public interface PaymentProvider {

    PaymentResult process(Payment payment);
}