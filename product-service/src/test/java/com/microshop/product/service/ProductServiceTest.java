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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository products;
    @Mock CategoryRepository categories;

    private ProductService service;
    private final Category books = Category.builder().id(UUID.randomUUID()).name("Books").build();

    @BeforeEach
    void setUp() {
        service = new ProductService(products, categories);
    }

    private Product product(String sku) {
        return Product.builder().id(UUID.randomUUID()).name("Phone").sku(sku)
                .price(new BigDecimal("10.00")).category(books).build();
    }

    private ProductRequest request(String sku, UUID categoryId) {
        return new ProductRequest("  Phone ", "desc", sku, new BigDecimal("199.99"), null, categoryId);
    }

    @Test
    void createNormalizesSkuAndSavesActiveProduct() {
        when(products.existsBySku("ELEC-1")).thenReturn(false);
        when(categories.findById(books.getId())).thenReturn(Optional.of(books));
        when(products.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ProductResponse res = service.create(request(" elec-1 ", books.getId()));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(products).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(res.sku()).isEqualTo("ELEC-1");
        assertThat(res.name()).isEqualTo("Phone");
        assertThat(res.categoryName()).isEqualTo("Books");
    }

    @Test
    void createRejectsDuplicateSku() {
        when(products.existsBySku("ELEC-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("elec-1", books.getId())))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(categories);
        verify(products, never()).save(any());
    }

    @Test
    void createRejectsUnknownCategory() {
        UUID missing = UUID.randomUUID();
        when(products.existsBySku("ELEC-1")).thenReturn(false);
        when(categories.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request("ELEC-1", missing)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");
    }

    @Test
    void updateChangesFieldsAndKeepsSameSkuWithoutUniquenessQuery() {
        Product existing = product("ELEC-1");
        when(products.findById(existing.getId())).thenReturn(Optional.of(existing));

        ProductResponse res = service.update(existing.getId(), request("elec-1", books.getId()));

        assertThat(existing.getName()).isEqualTo("Phone");
        assertThat(existing.getPrice()).isEqualByComparingTo("199.99");
        assertThat(res.price()).isEqualByComparingTo("199.99");
        verify(products, never()).existsBySkuAndIdNot(any(), any());
    }

    @Test
    void updateRejectsSkuUsedByAnotherProduct() {
        Product existing = product("A-1");
        when(products.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(products.existsBySkuAndIdNot("B-2", existing.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.update(existing.getId(), request("b-2", books.getId())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteIsSoft() {
        Product existing = product("ELEC-1");
        when(products.findById(existing.getId())).thenReturn(Optional.of(existing));

        service.delete(existing.getId());

        assertThat(existing.isActive()).isFalse();
        verify(products, never()).delete(any(Product.class));
    }

    @Test
    void inactiveProductsAreNotFound() {
        Product existing = product("ELEC-1");
        existing.setActive(false);
        when(products.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.getById(existing.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchRejectsUnsortableProperty() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("sku"));

        assertThatThrownBy(() -> service.search(ProductSearchCriteria.of(null, null, null, null), pageable))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(products);
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchMapsPageToResponse() {
        Pageable pageable = PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(products.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product("ELEC-1")), pageable, 1));

        PageResponse<ProductResponse> res = service.search(ProductSearchCriteria.of("pho", null, null, null), pageable);

        assertThat(res.totalElements()).isEqualTo(1);
        assertThat(res.content()).hasSize(1);
        assertThat(res.content().get(0).sku()).isEqualTo("ELEC-1");
    }
}
