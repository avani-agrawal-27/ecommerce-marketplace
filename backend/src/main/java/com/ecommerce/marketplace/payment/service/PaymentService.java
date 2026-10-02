package com.ecommerce.marketplace.payment.service;

import com.ecommerce.marketplace.common.exception.ForbiddenOperationException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.order.entity.Order;
import com.ecommerce.marketplace.order.entity.OrderStatus;
import com.ecommerce.marketplace.order.repository.OrderRepository;
import com.ecommerce.marketplace.payment.dto.CreatePaymentRequest;
import com.ecommerce.marketplace.payment.dto.PaymentResponse;
import com.ecommerce.marketplace.payment.entity.Payment;
import com.ecommerce.marketplace.payment.entity.PaymentStatus;
import com.ecommerce.marketplace.payment.provider.PaymentProvider;
import com.ecommerce.marketplace.payment.provider.PaymentResult;
import com.ecommerce.marketplace.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentProvider paymentProvider;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            PaymentProvider paymentProvider
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentProvider = paymentProvider;
    }

    @Transactional
    public PaymentResponse createPayment(
            UUID userId,
            CreatePaymentRequest request
    ) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + request.orderId()
                        )
                );

        if (!order.getUser().getId().equals(userId)) {
            throw new ForbiddenOperationException(
                    "You are not authorized to pay for this order"
            );
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException(
                    "Payment is not allowed for order status: "
                            + order.getStatus()
            );
        }

        if (paymentRepository.existsByOrderIdAndStatus(
                order.getId(),
                PaymentStatus.SUCCESS
        )) {
            throw new IllegalArgumentException(
                    "Order has already been paid"
            );
        }

        String paymentReference =
                "PAY-" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase();

        Payment payment = new Payment(
                order,
                paymentReference,
                "MOCK",
                order.getTotalAmount(),
                order.getCurrency()
        );

        payment.markProcessing();
        paymentRepository.save(payment);

        PaymentResult result = paymentProvider.process(payment);

        if (result.successful()) {
            payment.markSuccess();
            order.updateStatus(OrderStatus.CONFIRMED);
        } else {
            payment.markFailed(result.failureReason());
        }

        paymentRepository.save(payment);

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(
            UUID userId,
            UUID paymentId
    ) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found: " + paymentId
                        )
                );

        if (!payment.getOrder().getUser().getId().equals(userId)) {
            throw new ForbiddenOperationException(
                    "You are not authorized to view this payment"
            );
        }

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForOrder(
            UUID userId,
            UUID orderId
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        if (!order.getUser().getId().equals(userId)) {
            throw new ForbiddenOperationException(
                    "You are not authorized to view payments for this order"
            );
        }

        return paymentRepository
                .findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getPaymentReference(),
                payment.getProvider(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}