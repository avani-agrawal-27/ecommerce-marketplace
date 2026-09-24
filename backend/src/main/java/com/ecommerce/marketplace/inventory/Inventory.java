package com.ecommerce.marketplace.inventory;

import com.ecommerce.marketplace.common.exception.StockConflictException;
import com.ecommerce.marketplace.product.Product;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inventory_product", columnNames = "product_id")
        }
)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            unique = true
    )
    private Product product;

    @Column(nullable = false)
    private int quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Inventory() {
    }

    public Inventory(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.reservedQuantity = 0;
    }

    public UUID getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Stock quantity must be greater than zero"
            );
        }

        this.quantity += quantity;
    }

    public void removeStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Stock quantity must be greater than zero"
            );
        }

        if (getAvailableQuantity() < quantity) {
            throw new StockConflictException(
                    "Insufficient available stock"
            );
        }

        this.quantity -= quantity;
    }

    public void reserveStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Reservation quantity must be greater than zero"
            );
        }

        if (getAvailableQuantity() < quantity) {
            throw new StockConflictException(
                    "Insufficient available stock"
            );
        }

        this.reservedQuantity += quantity;
    }

    public void releaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Release quantity must be greater than zero"
            );
        }

        if (reservedQuantity < quantity) {
            throw new StockConflictException(
                    "Release quantity exceeds reserved stock"
            );
        }

        this.reservedQuantity -= quantity;
    }
}