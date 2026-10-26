package vn.team4.store.dto;

import vn.team4.store.model.BrandEntity;

import java.util.UUID;

public record BrandResponse(UUID id, String name, boolean isActive) {

    public static BrandResponse from(BrandEntity brand) {
        return new BrandResponse(brand.getId(), brand.getName(), brand.isActive());
    }
}
