package vn.team4.store.supplier.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SupplierUpdateRequest(
        String name,
        String address,
        @Pattern(regexp = "\\d{11}", message = "Số điện thoại phải gồm đúng 11 chữ số") String phone,
        @Email(message = "Email không hợp lệ") String email,
        @JsonProperty("isActive")
        @JsonDeserialize(using = StrictBooleanDeserializer.class)
        Boolean isActive) {
    public SupplierUpdateRequest {
        name = trim(name);
        address = trim(address);
        phone = trim(phone);
        email = trim(email);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
