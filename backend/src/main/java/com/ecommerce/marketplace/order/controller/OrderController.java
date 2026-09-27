package com.ecommerce.marketplace.order.controller;

import com.ecommerce.marketplace.order.dto.CreateOrderRequest;
import com.ecommerce.marketplace.order.dto.OrderResponse;
import com.ecommerce.marketplace.order.dto.UpdateOrderStatusRequest;
import com.ecommerce.marketplace.order.entity.OrderStatus;
import com.ecommerce.marketplace.order.service.OrderService;
import com.ecommerce.marketplace.user.repository.UserRepository;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt",
            "updatedAt",
            "totalAmount",
            "status"
    );

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(
            OrderService orderService,
            UserRepository userRepository
    ) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        UUID userId = getUserId(authentication);

        OrderResponse response = orderService.createOrder(
                userId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(
            Authentication authentication,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        UUID userId = getUserId(authentication);

        validatePagination(page, size);
        validateSort(sortBy, direction);

        Sort.Direction sortDirection =
                Sort.Direction.fromString(direction);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        return ResponseEntity.ok(
                orderService.getOrders(
                        userId,
                        status,
                        pageable
                )
        );
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            Authentication authentication,
            @PathVariable UUID orderId
    ) {
        UUID userId = getUserId(authentication);

        return ResponseEntity.ok(
                orderService.getOrder(
                        userId,
                        orderId
                )
        );
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            Authentication authentication,
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        validateStatusUpdatePermission(authentication);

        return ResponseEntity.ok(
                orderService.updateOrderStatus(
                        orderId,
                        request
                )
        );
    }

    private UUID getUserId(Authentication authentication) {

        return userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                )
                .getId();
    }

    private void validatePagination(
            int page,
            int size
    ) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to zero"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Size must be between 1 and 100"
            );
        }
    }

    private void validateSort(
            String sortBy,
            String direction
    ) {

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sort field: " + sortBy
            );
        }

        if (!direction.equalsIgnoreCase("asc")
                && !direction.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException(
                    "Sort direction must be asc or desc"
            );
        }
    }

    private void validateStatusUpdatePermission(
            Authentication authentication
    ) {

        boolean allowed = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                                || authority.getAuthority().equals("ROLE_SELLER")
                );

        if (!allowed) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only ADMIN or SELLER can update order status"
            );
        }
    }
}