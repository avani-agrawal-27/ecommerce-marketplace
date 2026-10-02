package com.ecommerce.marketplace.payment.repository;

import com.ecommerce.marketplace.payment.entity.Payment;
import com.ecommerce.marketplace.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    Optional<Payment> findByPaymentReference(String paymentReference);

    boolean existsByOrderIdAndStatus(
            UUID orderId,
            PaymentStatus status
    );
}