package vn.team4.store.dto;

import jakarta.validation.constraints.NotBlank;

public record BrandRequest(
        @NotBlank(message = "Tên thương hiệu không được để trống") String name,
        Boolean isActive) {
}
