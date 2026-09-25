package com.ecommerce.marketplace.product;

import com.ecommerce.marketplace.category.Category;
import com.ecommerce.marketplace.category.CategoryRepository;
import com.ecommerce.marketplace.common.exception.DuplicateResourceException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.product.dto.*;
import com.ecommerce.marketplace.product.entity.Product;
import com.ecommerce.marketplace.product.repository.ProductRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {

        if (productRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException(
                    "Product with this name already exists"
            );
        }

        if (productRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException(
                    "Product with this slug already exists"
            );
        }

        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException(
                    "Product with this SKU already exists"
            );
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found: " + request.categoryId()
                        )
                );

        Product product = new Product(
                category,
                request.name(),
                request.slug(),
                request.description(),
                request.sku(),
                request.price()
        );

        Product savedProduct = productRepository.save(product);

        return toResponse(savedProduct);
    }

    public ProductPageResponse getAllProducts(
            UUID categoryId,
            Boolean active,
            String search,
            Pageable pageable){
        Specification<Product> specification = null;

        if (categoryId != null) {
            specification = ProductSpecification.hasCategory(categoryId);
        }

        if (active != null) {
            Specification<Product> activeSpecification =
                    ProductSpecification.hasActiveStatus(active);

            specification = specification == null
                    ? activeSpecification
                    : specification.and(activeSpecification);
        }

        if (search != null && !search.isBlank()) {
            Specification<Product> searchSpecification =
                    ProductSpecification.search(search);

            specification = specification == null
                    ? searchSpecification
                    : specification.and(searchSpecification);
        }

        Page<Product> productPage =
                productRepository.findAll(specification, pageable);

        List<ProductResponse> products = productPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new ProductPageResponse(
                products,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isFirst(),
                productPage.isLast()
        );
    }
    private ProductResponse toResponse(Product product) {

        return new ProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getSku(),
                product.getPrice(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found: " + id)
                );

        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(
            UUID id,
            UpdateProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found: " + id)
                );

        if (productRepository.existsByNameIgnoreCaseAndIdNot(
                request.name(), id)) {

            throw new DuplicateResourceException(
                    "Product with this name already exists"
            );
        }

        if (productRepository.existsBySlugAndIdNot(
                request.slug(), id)) {

            throw new DuplicateResourceException(
                    "Product with this slug already exists"
            );
        }

        if (productRepository.existsBySkuAndIdNot(
                request.sku(), id)) {

            throw new DuplicateResourceException(
                    "Product with this SKU already exists"
            );
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found: " + request.categoryId()
                        )
                );

        product.update(
                category,
                request.name(),
                request.slug(),
                request.description(),
                request.sku(),
                request.price()
        );

        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateProductStatus(
            UUID id,
            UpdateProductStatusRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found: " + id)
                );

        product.updateStatus(request.active());

        return toResponse(product);
    }
}