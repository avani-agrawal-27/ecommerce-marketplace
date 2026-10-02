package com.ecommerce.marketplace.payment.provider;

import com.ecommerce.marketplace.payment.entity.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentProvider implements PaymentProvider {

    private final boolean simulateFailure;

    public MockPaymentProvider(
            @Value("${payment.mock.simulate-failure:false}")
            boolean simulateFailure
    ) {
        this.simulateFailure = simulateFailure;
    }

    @Override
    public PaymentResult process(Payment payment) {

        if (simulateFailure) {
            return PaymentResult.failure(
                    "Mock payment declined"
            );
        }

        return PaymentResult.success();
    }
}