package vn.team4.store.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(@NotBlank(message = "Tên danh mục không được để trống") String name) {
}
