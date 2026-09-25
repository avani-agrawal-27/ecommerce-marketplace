package com.ecommerce.marketplace.cart.service;

import com.ecommerce.marketplace.cart.dto.AddCartItemRequest;
import com.ecommerce.marketplace.cart.dto.CartItemResponse;
import com.ecommerce.marketplace.cart.dto.CartResponse;
import com.ecommerce.marketplace.cart.dto.UpdateCartItemRequest;
import com.ecommerce.marketplace.cart.entity.Cart;
import com.ecommerce.marketplace.cart.entity.CartItem;
import com.ecommerce.marketplace.cart.repository.CartItemRepository;
import com.ecommerce.marketplace.cart.repository.CartRepository;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.inventory.entity.Inventory;
import com.ecommerce.marketplace.inventory.repository.InventoryRepository;
import com.ecommerce.marketplace.product.entity.Product;
import com.ecommerce.marketplace.product.repository.ProductRepository;
import com.ecommerce.marketplace.user.entity.User;
import com.ecommerce.marketplace.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        Cart cart = cartRepository.findWithItemsByUserId(userId)
                .orElseGet(() -> createCart(userId));

        return toCartResponse(cart);
    }

    public CartResponse addItem(
            UUID userId,
            AddCartItemRequest request
    ) {
        Cart cart = getOrCreateCart(userId);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found: " + request.productId()
                        )
                );

        validateProduct(product);

        Inventory inventory = inventoryRepository
                .findByProductId(product.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inventory not found for product: "
                                        + product.getId()
                        )
                );

        CartItem existingItem =
                cartItemRepository
                        .findByCartIdAndProductId(
                                cart.getId(),
                                product.getId()
                        )
                        .orElse(null);

        int newQuantity = request.quantity();

        if (existingItem != null) {
            newQuantity = existingItem.getQuantity()
                    + request.quantity();

            validateQuantity(
                    newQuantity,
                    inventory,
                    product
            );

            existingItem.updateQuantity(newQuantity);

        } else {
            validateQuantity(
                    newQuantity,
                    inventory,
                    product
            );

            CartItem cartItem =
                    new CartItem(product, newQuantity);

            cart.addItem(cartItem);
        }

        cartRepository.save(cart);

        return toCartResponse(cart);
    }

    public CartResponse updateItem(
            UUID userId,
            UUID productId,
            UpdateCartItemRequest request
    ) {
        Cart cart = getCartEntity(userId);

        CartItem cartItem =
                cartItemRepository
                        .findByCartIdAndProductId(
                                cart.getId(),
                                productId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Product is not in the cart: "
                                                + productId
                                )
                        );

        Product product = cartItem.getProduct();

        validateProduct(product);

        Inventory inventory = inventoryRepository
                .findByProductId(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inventory not found for product: "
                                        + productId
                        )
                );

        validateQuantity(
                request.quantity(),
                inventory,
                product
        );

        cartItem.updateQuantity(request.quantity());

        return toCartResponse(cart);
    }

    public CartResponse removeItem(
            UUID userId,
            UUID productId
    ) {
        Cart cart = getCartEntity(userId);

        CartItem cartItem =
                cartItemRepository
                        .findByCartIdAndProductId(
                                cart.getId(),
                                productId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Product is not in the cart: "
                                                + productId
                                )
                        );

        cart.removeItem(cartItem);

        return toCartResponse(cart);
    }

    public void clearCart(UUID userId) {
        Cart cart = getCartEntity(userId);

        /*
         * Copy the collection before removing items to avoid
         * modifying the collection while iterating over it.
         */
        for (CartItem item : new ArrayList<>(cart.getItems())) {
            cart.removeItem(item);
        }
    }

    private Cart getOrCreateCart(UUID userId) {
        return cartRepository.findWithItemsByUserId(userId)
                .orElseGet(() -> createCart(userId));
    }

    private Cart getCartEntity(UUID userId) {
        return cartRepository.findWithItemsByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cart not found for user: " + userId
                        )
                );
    }

    private Cart createCart(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + userId
                        )
                );

        Cart cart = new Cart(user);

        return cartRepository.save(cart);
    }

    private void validateProduct(Product product) {
        if (!product.isActive()) {
            throw new IllegalArgumentException(
                    "Product is not active: " + product.getId()
            );
        }
    }

    private void validateQuantity(
            int quantity,
            Inventory inventory,
            Product product
    ) {
        if (quantity > inventory.getAvailableQuantity()) {
            throw new IllegalArgumentException(
                    "Requested quantity for product "
                            + product.getId()
                            + " exceeds available inventory"
            );
        }
    }

    private CartResponse toCartResponse(Cart cart) {

        List<CartItemResponse> items =
                cart.getItems()
                        .stream()
                        .map(item -> {

                            BigDecimal unitPrice =
                                    item.getProduct().getPrice();

                            BigDecimal subtotal =
                                    unitPrice.multiply(
                                            BigDecimal.valueOf(
                                                    item.getQuantity()
                                            )
                                    );

                            return new CartItemResponse(
                                    item.getId(),
                                    item.getProduct().getId(),
                                    item.getProduct().getName(),
                                    item.getProduct().getSku(),
                                    unitPrice,
                                    item.getQuantity(),
                                    subtotal
                            );
                        })
                        .toList();

        BigDecimal total =
                items.stream()
                        .map(CartItemResponse::subtotal)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return new CartResponse(
                cart.getId(),
                cart.getUser().getId(),
                items,
                total
        );
    }
}