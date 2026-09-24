package com.ecommerce.marketplace.inventory;

import com.ecommerce.marketplace.inventory.dto.AdjustStockRequest;
import com.ecommerce.marketplace.inventory.dto.CreateInventoryRequest;
import com.ecommerce.marketplace.inventory.dto.InventoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(
            InventoryService inventoryService) {

        this.inventoryService = inventoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResponse createInventory(
            @Valid @RequestBody CreateInventoryRequest request) {

        return inventoryService.createInventory(request);
    }

    @GetMapping
    public List<InventoryResponse> getAllInventory() {

        return inventoryService.getAllInventory();
    }

    @GetMapping("/{productId}")
    public InventoryResponse getInventory(
            @PathVariable UUID productId) {

        return inventoryService.getInventoryByProductId(productId);
    }

    @PatchMapping("/{productId}/stock/add")
    public InventoryResponse addStock(
            @PathVariable UUID productId,
            @Valid @RequestBody AdjustStockRequest request) {

        return inventoryService.addStock(
                productId,
                request
        );
    }

    @PatchMapping("/{productId}/stock/remove")
    public InventoryResponse removeStock(
            @PathVariable UUID productId,
            @Valid @RequestBody AdjustStockRequest request) {

        return inventoryService.removeStock(
                productId,
                request
        );
    }
}