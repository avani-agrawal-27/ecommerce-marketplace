package com.ecommerce.marketplace.product;

import com.ecommerce.marketplace.product.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "name",
                    "price",
                    "createdAt",
                    "updatedAt"
            );

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(
            @Valid @RequestBody CreateProductRequest request) {

        return productService.createProduct(request);
    }

    @GetMapping
    public ProductPageResponse getAllProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number must not be negative"
            );
        }

        if (size < 1 || size > 50) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 50"
            );
        }

        Pageable pageable = createPageable(page, size, sort);

        return productService.getAllProducts(
                categoryId,
                active,
                search,
                pageable
        );
    }

    private Pageable createPageable(
            int page,
            int size,
            String sort) {

        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size);
        }

        String[] sortParts = sort.split(",");

        String property = sortParts[0].trim();

        if (!ALLOWED_SORT_FIELDS.contains(property)) {
            throw new IllegalArgumentException(
                    "Invalid sort field: " + property
            );
        }

        Sort.Direction direction = Sort.Direction.ASC;

        if (sortParts.length > 1) {
            try {
                direction = Sort.Direction.fromString(
                        sortParts[1].trim()
                );
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Sort direction must be 'asc' or 'desc'"
                );
            }
        }

        if (sortParts.length > 2) {
            throw new IllegalArgumentException(
                    "Invalid sort parameter"
            );
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(direction, property)
        );
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable UUID id) {
        return productService.getProductById(id);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {

        return productService.updateProduct(id, request);
    }

    @PatchMapping("/{id}/status")
    public ProductResponse updateProductStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductStatusRequest request) {

        return productService.updateProductStatus(id, request);
    }
}