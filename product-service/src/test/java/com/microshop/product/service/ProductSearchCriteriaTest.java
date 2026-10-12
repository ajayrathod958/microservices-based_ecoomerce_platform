package com.microshop.product.service;

import com.microshop.common.exception.BadRequestException;
import com.microshop.product.dto.ProductSearchCriteria;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductSearchCriteriaTest {

    @Test
    void trimsAndLowercasesTermAndTreatsBlankAsNone() {
        assertThat(ProductSearchCriteria.of("  KeyBoard ", null, null, null).q()).isEqualTo("keyboard");
        assertThat(ProductSearchCriteria.of("   ", null, null, null).q()).isNull();
    }

    @Test
    void rejectsOverlongTermAndInvertedPriceRange() {
        assertThatThrownBy(() -> ProductSearchCriteria.of("x".repeat(101), null, null, null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> ProductSearchCriteria.of(null, null, new BigDecimal("50"), new BigDecimal("10")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> ProductSearchCriteria.of(null, null, new BigDecimal("-1"), null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void cacheKeyIsStableForSameInputAndDiffersWhenAnythingChanges() {
        var page0 = PageRequest.of(0, 12, Sort.by("price"));
        var a = ProductSearchCriteria.of("Phone", null, new BigDecimal("10.0"), null);
        var sameLogically = ProductSearchCriteria.of(" phone ", null, new BigDecimal("10"), null);

        assertThat(a.cacheKey(page0)).isEqualTo(sameLogically.cacheKey(page0));
        assertThat(a.cacheKey(page0)).isNotEqualTo(a.cacheKey(PageRequest.of(1, 12, Sort.by("price"))));
        assertThat(a.cacheKey(page0)).isNotEqualTo(a.cacheKey(PageRequest.of(0, 12, Sort.by("name"))));
        assertThat(a.cacheKey(page0)).isNotEqualTo(
                ProductSearchCriteria.of("tablet", null, new BigDecimal("10"), null).cacheKey(page0));
    }
}
