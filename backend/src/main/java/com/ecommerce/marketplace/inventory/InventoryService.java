package com.ecommerce.marketplace.inventory;

import com.ecommerce.marketplace.common.exception.DuplicateResourceException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.inventory.dto.AdjustStockRequest;
import com.ecommerce.marketplace.inventory.dto.CreateInventoryRequest;
import com.ecommerce.marketplace.inventory.dto.InventoryResponse;
import com.ecommerce.marketplace.product.Product;
import com.ecommerce.marketplace.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryService(
            InventoryRepository inventoryRepository,
            ProductRepository productRepository) {

        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public InventoryResponse createInventory(
            CreateInventoryRequest request) {

        if (inventoryRepository.existsByProductId(request.productId())) {
            throw new DuplicateResourceException(
                    "Inventory already exists for this product"
            );
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found: " + request.productId()
                        )
                );

        Inventory inventory = new Inventory(
                product,
                request.quantity()
        );

        Inventory savedInventory =
                inventoryRepository.save(inventory);

        return toResponse(savedInventory);
    }

    public InventoryResponse getInventoryByProductId(
            java.util.UUID productId) {

        Inventory inventory =
                inventoryRepository.findByProductId(productId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Inventory not found for product: "
                                                + productId
                                )
                        );

        return toResponse(inventory);
    }

    public List<InventoryResponse> getAllInventory() {

        return inventoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InventoryResponse addStock(
            java.util.UUID productId,
            AdjustStockRequest request) {

        Inventory inventory =
                inventoryRepository.findByProductIdForUpdate(productId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Inventory not found for product: "
                                                + productId
                                )
                        );

        inventory.addStock(request.quantity());

        inventoryRepository.flush();

        return toResponse(inventory);
    }

    @Transactional
    public InventoryResponse removeStock(
            java.util.UUID productId,
            AdjustStockRequest request) {

        Inventory inventory =
                inventoryRepository.findByProductIdForUpdate(productId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Inventory not found for product: "
                                                + productId
                                )
                        );

        inventory.removeStock(request.quantity());

        inventoryRepository.flush();

        return toResponse(inventory);
    }

    private InventoryResponse toResponse(
            Inventory inventory) {

        return new InventoryResponse(
                inventory.getId(),
                inventory.getProduct().getId(),
                inventory.getProduct().getName(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }
}