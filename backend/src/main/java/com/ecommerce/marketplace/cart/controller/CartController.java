package com.ecommerce.marketplace.cart.controller;

import com.ecommerce.marketplace.cart.dto.AddCartItemRequest;
import com.ecommerce.marketplace.cart.dto.CartResponse;
import com.ecommerce.marketplace.cart.dto.UpdateCartItemRequest;
import com.ecommerce.marketplace.cart.service.CartService;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    public CartController(
            CartService cartService,
            UserRepository userRepository
    ) {
        this.cartService = cartService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            Authentication authentication
    ) {
        UUID userId = getUserId(authentication);

        return ResponseEntity.ok(
                cartService.getCart(userId)
        );
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            Authentication authentication,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        UUID userId = getUserId(authentication);

        return ResponseEntity.ok(
                cartService.addItem(userId, request)
        );
    }

    @PatchMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItem(
            Authentication authentication,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        UUID userId = getUserId(authentication);

        return ResponseEntity.ok(
                cartService.updateItem(userId, productId, request)
        );
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(
            Authentication authentication,
            @PathVariable UUID productId
    ) {
        UUID userId = getUserId(authentication);

        return ResponseEntity.ok(
                cartService.removeItem(userId, productId)
        );
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            Authentication authentication
    ) {
        UUID userId = getUserId(authentication);

        cartService.clearCart(userId);

        return ResponseEntity.noContent().build();
    }

    private UUID getUserId(Authentication authentication) {

        return userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                )
                .getId();
    }
}