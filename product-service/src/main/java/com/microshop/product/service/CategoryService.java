package com.microshop.product.service;

import com.microshop.common.exception.ConflictException;
import com.microshop.common.exception.ResourceNotFoundException;
import com.microshop.product.dto.CategoryRequest;
import com.microshop.product.dto.CategoryResponse;
import com.microshop.product.entity.Category;
import com.microshop.product.repository.CategoryRepository;
import com.microshop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.microshop.product.config.CacheConfig.PRODUCT;
import static com.microshop.product.config.CacheConfig.PRODUCT_PAGE;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categories;
    private final ProductRepository products;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categories.findAll(Sort.by("name")).stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID id) {
        return CategoryResponse.from(find(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        String name = req.name().trim();
        if (categories.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Category '" + name + "' already exists");
        }
        return CategoryResponse.from(categories.save(
                Category.builder().name(name).description(req.description()).build()));
    }

    /** Product responses embed the category name, so a rename invalidates every cached product. */
    @Caching(evict = {
            @CacheEvict(cacheNames = PRODUCT, allEntries = true),
            @CacheEvict(cacheNames = PRODUCT_PAGE, allEntries = true)})
    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest req) {
        Category category = find(id);
        String name = req.name().trim();
        if (categories.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Category '" + name + "' already exists");
        }
        category.setName(name);
        category.setDescription(req.description());
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(UUID id) {
        Category category = find(id);
        if (products.existsByCategoryId(id)) {
            throw new ConflictException("Category still has products; move or delete them first");
        }
        categories.delete(category);
    }

    private Category find(UUID id) {
        return categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }
}
