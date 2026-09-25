package com.ecommerce.marketplace.product;

import com.ecommerce.marketplace.category.Category;
import com.ecommerce.marketplace.product.entity.Product;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> hasCategory(UUID categoryId) {
        return (root, query, criteriaBuilder) -> {

            Join<Product, Category> category =
                    root.join("category", JoinType.INNER);

            return criteriaBuilder.equal(
                    category.get("id"),
                    categoryId
            );
        };
    }

    public static Specification<Product> hasActiveStatus(boolean active) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("active"),
                        active
                );
    }

    public static Specification<Product> search(String search) {
        return (root, query, criteriaBuilder) -> {

            String searchPattern = "%" + search.trim().toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("name")),
                            searchPattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("description")),
                            searchPattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("sku")),
                            searchPattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("slug")),
                            searchPattern
                    )
            );
        };
    }
}