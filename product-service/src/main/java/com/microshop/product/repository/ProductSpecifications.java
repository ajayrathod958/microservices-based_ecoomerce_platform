package com.microshop.product.repository;

import com.microshop.product.dto.ProductSearchCriteria;
import com.microshop.product.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> from(ProductSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (c.q() != null) {
                String like = "%" + escapeLike(c.q()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like, '\\'),
                        cb.like(cb.lower(root.get("description")), like, '\\')));
            }
            if (c.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), c.categoryId()));
            }
            if (c.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), c.minPrice()));
            }
            if (c.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), c.maxPrice()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Makes user input literal inside a LIKE pattern (escape char is backslash). */
    static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
