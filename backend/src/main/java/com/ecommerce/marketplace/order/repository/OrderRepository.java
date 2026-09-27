package com.ecommerce.marketplace.order.repository;

import com.ecommerce.marketplace.order.entity.Order;
import com.ecommerce.marketplace.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = {
            "items",
            "items.product"
    })
    Optional<Order> findWithItemsById(UUID id);

    @EntityGraph(attributePaths = {
            "items",
            "items.product"
    })
    Optional<Order> findWithItemsByIdAndUserId(UUID id, UUID userId);

    @EntityGraph(attributePaths = {
            "items",
            "items.product"
    })
    Page<Order> findByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "items",
            "items.product"
    })
    Page<Order> findByUserIdAndStatus(
            UUID userId,
            OrderStatus status,
            Pageable pageable
    );
}