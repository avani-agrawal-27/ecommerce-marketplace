package com.ecommerce.marketplace.category;

import com.ecommerce.marketplace.category.dto.CategoryResponse;
import com.ecommerce.marketplace.category.dto.CreateCategoryRequest;
import com.ecommerce.marketplace.common.exception.DuplicateResourceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {

        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException(
                    "Category with this name already exists"
            );
        }

        if (categoryRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException(
                    "Category with this slug already exists"
            );
        }

        Category category = new Category(
                request.name(),
                request.slug(),
                request.description()
        );

        Category savedCategory = categoryRepository.save(category);

        return toResponse(savedCategory);
    }

    public List<CategoryResponse> getAllCategories() {

        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CategoryResponse toResponse(Category category) {

        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}