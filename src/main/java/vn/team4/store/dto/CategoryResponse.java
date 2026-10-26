package vn.team4.store.dto;

import vn.team4.store.model.CategoryEntity;

import java.util.UUID;

public record CategoryResponse(UUID id, String name) {

    public static CategoryResponse from(CategoryEntity category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
