package vn.team4.store.dto;

import jakarta.validation.constraints.Email;

public record SupplierUpdateRequest(
        String name,
        String address,
        String phone,
        @Email(message = "Email không hợp lệ") String email,
        Boolean isActive) {
}
