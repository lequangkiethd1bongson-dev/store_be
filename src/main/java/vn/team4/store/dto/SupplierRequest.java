package vn.team4.store.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
        @NotBlank(message = "Tên nhà cung cấp không được để trống") String name,
        String address,
        String phone,
        @Email(message = "Email không hợp lệ") String email,
        Boolean isActive) {
}
