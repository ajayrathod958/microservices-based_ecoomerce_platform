package com.microshop.product.dto;

import com.microshop.product.entity.Category;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, String description) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription());
    }
}
