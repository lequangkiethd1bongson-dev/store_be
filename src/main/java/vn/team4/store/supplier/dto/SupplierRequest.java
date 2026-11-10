package vn.team4.store.supplier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SupplierRequest(
        @NotBlank(message = "Tên nhà cung cấp không được để trống") String name,
        @NotBlank(message = "Địa chỉ không được để trống") String address,
        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "\\d{11}", message = "Số điện thoại phải gồm đúng 11 chữ số") String phone,
        @Email(message = "Email không hợp lệ") String email) {
    public SupplierRequest {
        name = trim(name);
        address = trim(address);
        phone = trim(phone);
        email = trim(email);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
