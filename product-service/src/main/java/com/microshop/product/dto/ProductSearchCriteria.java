package com.microshop.product.dto;

import com.microshop.common.exception.BadRequestException;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Normalised, validated search filters; also the basis of the listing cache key. */
public record ProductSearchCriteria(String q, UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice) {

    private static final int MAX_TERM_LENGTH = 100;

    public static ProductSearchCriteria of(String q, UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        String term = (q == null || q.isBlank()) ? null : q.trim().toLowerCase();
        if (term != null && term.length() > MAX_TERM_LENGTH) {
            throw new BadRequestException("Search term must be at most " + MAX_TERM_LENGTH + " characters");
        }
        if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)) {
            throw new BadRequestException("Prices must not be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice must not exceed maxPrice");
        }
        return new ProductSearchCriteria(term, categoryId, minPrice, maxPrice);
    }

    /** Fixed-length, Redis-safe key: same filters + same page/sort => same key. */
    public String cacheKey(Pageable pageable) {
        String raw = Stream.of(
                        nz(q), nz(categoryId), plain(minPrice), plain(maxPrice),
                        String.valueOf(pageable.getPageNumber()), String.valueOf(pageable.getPageSize()),
                        pageable.getSort().toString())
                .collect(Collectors.joining("|"));
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String nz(Object o) {
        return o == null ? "-" : o.toString();
    }

    private static String plain(BigDecimal d) {
        return d == null ? "-" : d.stripTrailingZeros().toPlainString();
    }
}
