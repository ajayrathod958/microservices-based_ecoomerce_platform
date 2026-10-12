package com.microshop.product.service;

import com.microshop.common.api.PageResponse;
import com.microshop.common.exception.BadRequestException;
import com.microshop.common.exception.ConflictException;
import com.microshop.common.exception.ResourceNotFoundException;
import com.microshop.product.dto.ProductRequest;
import com.microshop.product.dto.ProductResponse;
import com.microshop.product.dto.ProductSearchCriteria;
import com.microshop.product.entity.Category;
import com.microshop.product.entity.Product;
import com.microshop.product.repository.CategoryRepository;
import com.microshop.product.repository.ProductRepository;
import com.microshop.product.repository.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static com.microshop.product.config.CacheConfig.PRODUCT;
import static com.microshop.product.config.CacheConfig.PRODUCT_PAGE;

/**
 * Caching strategy (cache-aside):
 *  - reads: product details by id, and listings keyed by filters+page+sort;
 *  - writes: evict that product's entry and ALL listings (any listing may contain it or sort differently now).
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Set<String> SORTABLE = Set.of("name", "price", "createdAt", "updatedAt");

    private final ProductRepository products;
    private final CategoryRepository categories;

    @Cacheable(cacheNames = PRODUCT_PAGE, key = "#criteria.cacheKey(#pageable)")
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(ProductSearchCriteria criteria, Pageable pageable) {
        validateSort(pageable);
        Page<ProductResponse> page = products
                .findAll(ProductSpecifications.from(criteria), pageable)
                .map(ProductResponse::from);
        return PageResponse.from(page);
    }

    @Cacheable(cacheNames = PRODUCT, key = "#id.toString()")
    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        return products.findById(id)
                .filter(Product::isActive)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    @CacheEvict(cacheNames = PRODUCT_PAGE, allEntries = true)
    @Transactional
    public ProductResponse create(ProductRequest req) {
        String sku = normalizeSku(req.sku());
        if (products.existsBySku(sku)) {
            throw new ConflictException("SKU '" + sku + "' already exists");
        }
        Category category = findCategory(req.categoryId());
        Product saved = products.save(Product.builder()
                .name(req.name().trim())
                .description(req.description())
                .sku(sku)
                .price(req.price())
                .imageUrl(req.imageUrl())
                .category(category)
                .build());
        return ProductResponse.from(saved);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = PRODUCT, key = "#id.toString()"),
            @CacheEvict(cacheNames = PRODUCT_PAGE, allEntries = true)})
    @Transactional
    public ProductResponse update(UUID id, ProductRequest req) {
        Product product = findActive(id);
        String sku = normalizeSku(req.sku());
        if (!sku.equals(product.getSku()) && products.existsBySkuAndIdNot(sku, id)) {
            throw new ConflictException("SKU '" + sku + "' already exists");
        }
        if (!req.categoryId().equals(product.getCategory().getId())) {
            product.setCategory(findCategory(req.categoryId()));
        }
        product.setName(req.name().trim());
        product.setDescription(req.description());
        product.setSku(sku);
        product.setPrice(req.price());
        product.setImageUrl(req.imageUrl());
        return ProductResponse.from(product);
    }

    /** Soft delete: the product disappears from the shop but historical orders can still reference it. */
    @Caching(evict = {
            @CacheEvict(cacheNames = PRODUCT, key = "#id.toString()"),
            @CacheEvict(cacheNames = PRODUCT_PAGE, allEntries = true)})
    @Transactional
    public void delete(UUID id) {
        findActive(id).setActive(false);
    }

    private Product findActive(UUID id) {
        return products.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private Category findCategory(UUID id) {
        return categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private static String normalizeSku(String sku) {
        return sku.trim().toUpperCase();
    }

    private static void validateSort(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new BadRequestException(
                        "Cannot sort by '" + order.getProperty() + "'. Allowed: " + String.join(", ", SORTABLE));
            }
        }
    }
}
