package com.ecommerce.marketplace.order.service;

import com.ecommerce.marketplace.cart.entity.Cart;
import com.ecommerce.marketplace.cart.entity.CartItem;
import com.ecommerce.marketplace.cart.repository.CartRepository;
import com.ecommerce.marketplace.inventory.entity.Inventory;
import com.ecommerce.marketplace.inventory.repository.InventoryRepository;
import com.ecommerce.marketplace.order.dto.CreateOrderRequest;
import com.ecommerce.marketplace.order.dto.OrderItemResponse;
import com.ecommerce.marketplace.order.dto.OrderResponse;
import com.ecommerce.marketplace.order.dto.UpdateOrderStatusRequest;
import com.ecommerce.marketplace.order.entity.Order;
import com.ecommerce.marketplace.order.entity.OrderItem;
import com.ecommerce.marketplace.order.entity.OrderStatus;
import com.ecommerce.marketplace.order.repository.OrderRepository;
import com.ecommerce.marketplace.product.entity.Product;
import com.ecommerce.marketplace.user.entity.User;
import com.ecommerce.marketplace.user.repository.UserRepository;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.common.exception.StockConflictException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final InventoryRepository inventoryRepository;

    public OrderService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            CartRepository cartRepository,
            InventoryRepository inventoryRepository
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public OrderResponse createOrder(
            UUID userId,
            CreateOrderRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found: " + userId)
                );

        Cart cart = cartRepository.findWithItemsByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cart not found for user: " + userId
                        )
                );

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot create an order from an empty cart"
            );
        }

        /*
         * Lock inventory rows in deterministic product-id order.
         *
         * This reduces the possibility of deadlocks when two users
         * are ordering overlapping products concurrently.
         */
        List<CartItem> cartItems = new ArrayList<>(cart.getItems());

        cartItems.sort(
                Comparator.comparing(
                        item -> item.getProduct().getId()
                )
        );

        BigDecimal subtotal = BigDecimal.ZERO;

        List<Inventory> lockedInventories = new ArrayList<>();

        for (CartItem cartItem : cartItems) {

            Product product = cartItem.getProduct();

            if (!product.isActive()) {
                throw new IllegalArgumentException(
                        "Product is no longer active: " + product.getName()
                );
            }

            Inventory inventory = inventoryRepository
                    .findByProductIdForUpdate(product.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Inventory not found for product: "
                                            + product.getId()
                            )
                    );

            if (cartItem.getQuantity() > inventory.getAvailableQuantity()) {
                throw new StockConflictException(
                        "Insufficient inventory for product: "
                                + product.getName()
                                + ". Available: "
                                + inventory.getAvailableQuantity()
                                + ", requested: "
                                + cartItem.getQuantity()
                );
            }

            lockedInventories.add(inventory);

            BigDecimal lineTotal = product.getPrice()
                    .multiply(
                            BigDecimal.valueOf(cartItem.getQuantity())
                    );

            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal shippingFee = calculateShippingFee(subtotal);
        BigDecimal totalAmount = subtotal.add(shippingFee);

        Order order = new Order(
                user,
                generateOrderNumber(),
                subtotal,
                shippingFee,
                totalAmount,
                request.shippingFirstName().trim(),
                normalizeOptional(request.shippingLastName()),
                request.shippingAddressLine1().trim(),
                normalizeOptional(request.shippingAddressLine2()),
                request.shippingCity().trim(),
                request.shippingState().trim(),
                request.shippingPostalCode().trim(),
                request.shippingCountry().trim(),
                request.shippingPhone().trim()
        );

        for (int i = 0; i < cartItems.size(); i++) {

            CartItem cartItem = cartItems.get(i);
            Inventory inventory = lockedInventories.get(i);
            Product product = cartItem.getProduct();

            BigDecimal lineTotal = product.getPrice()
                    .multiply(
                            BigDecimal.valueOf(cartItem.getQuantity())
                    );

            OrderItem orderItem = new OrderItem(
                    product,
                    product.getName(),
                    product.getSku(),
                    product.getPrice(),
                    cartItem.getQuantity(),
                    lineTotal
            );

            order.addItem(orderItem);

            /*
             * Reserve the quantity only after the inventory row
             * has been locked and validated.
             */
            inventory.reserveStock(cartItem.getQuantity());
        }

        Order savedOrder = orderRepository.save(order);

        /*
         * Clear the cart only after all inventory reservations
         * and order items have been successfully prepared.
         */
        for (CartItem cartItem : new ArrayList<>(cart.getItems())) {
            cart.removeItem(cartItem);
        }

        return toOrderResponse(savedOrder);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public OrderResponse getOrder(
            UUID userId,
            UUID orderId
    ) {
        Order order = orderRepository
                .findWithItemsByIdAndUserId(orderId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        return toOrderResponse(order);
    }

    /*
     * Order list needs items and products while building the response.
     * The repository methods use EntityGraph to fetch them.
     */
    @Transactional
    public Page<OrderResponse> getOrders(
            UUID userId,
            OrderStatus status,
            Pageable pageable
    ) {
        Page<Order> orders;

        if (status != null) {
            orders = orderRepository.findByUserIdAndStatus(
                    userId,
                    status,
                    pageable
            );
        } else {
            orders = orderRepository.findByUserId(
                    userId,
                    pageable
            );
        }

        return orders.map(this::toOrderResponse);
    }

    public OrderResponse updateOrderStatus(
            UUID orderId,
            UpdateOrderStatusRequest request
    ) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        validateStatusTransition(
                order.getStatus(),
                request.status()
        );

        /*
         * When an order is cancelled, release the inventory
         * that was reserved during order creation.
         */
        if (request.status() == OrderStatus.CANCELLED) {

            for (OrderItem item : order.getItems()) {

                Inventory inventory = inventoryRepository
                        .findByProductIdForUpdate(
                                item.getProduct().getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Inventory not found for product: "
                                                + item.getProduct().getId()
                                )
                        );

                inventory.releaseStock(item.getQuantity());
            }
        }

        order.updateStatus(request.status());

        return toOrderResponse(order);
    }

    private void validateStatusTransition(
            OrderStatus currentStatus,
            OrderStatus newStatus
    ) {

        if (currentStatus == newStatus) {
            throw new IllegalArgumentException(
                    "Order is already in status: " + currentStatus
            );
        }

        switch (currentStatus) {

            case PENDING_PAYMENT -> {
                if (newStatus != OrderStatus.CONFIRMED
                        && newStatus != OrderStatus.CANCELLED) {
                    throw invalidTransition(currentStatus, newStatus);
                }
            }

            case CONFIRMED -> {
                if (newStatus != OrderStatus.PROCESSING
                        && newStatus != OrderStatus.CANCELLED) {
                    throw invalidTransition(currentStatus, newStatus);
                }
            }

            case PROCESSING -> {
                if (newStatus != OrderStatus.SHIPPED
                        && newStatus != OrderStatus.CANCELLED) {
                    throw invalidTransition(currentStatus, newStatus);
                }
            }

            case SHIPPED -> {
                if (newStatus != OrderStatus.DELIVERED) {
                    throw invalidTransition(currentStatus, newStatus);
                }
            }

            case DELIVERED, CANCELLED -> throw new IllegalArgumentException(
                    "Order cannot transition from status: " + currentStatus
            );
        }
    }

    private IllegalArgumentException invalidTransition(
            OrderStatus currentStatus,
            OrderStatus newStatus
    ) {
        return new IllegalArgumentException(
                "Invalid order status transition from "
                        + currentStatus
                        + " to "
                        + newStatus
        );
    }

    private BigDecimal calculateShippingFee(BigDecimal subtotal) {

        /*
         * Simple marketplace rule for V1:
         * Orders of ₹5,000 or more get free shipping.
         * Otherwise shipping is ₹100.
         */
        if (subtotal.compareTo(BigDecimal.valueOf(5000)) >= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(100);
    }

    private String generateOrderNumber() {

        return "ORD-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }

    private String normalizeOptional(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private OrderResponse toOrderResponse(Order order) {

        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProductName(),
                        item.getSku(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getLineTotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUser().getId(),
                order.getStatus(),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getShippingFirstName(),
                order.getShippingLastName(),
                order.getShippingAddressLine1(),
                order.getShippingAddressLine2(),
                order.getShippingCity(),
                order.getShippingState(),
                order.getShippingPostalCode(),
                order.getShippingCountry(),
                order.getShippingPhone(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}