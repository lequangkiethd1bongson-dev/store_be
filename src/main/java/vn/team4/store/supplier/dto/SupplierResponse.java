package vn.team4.store.supplier.dto;

import vn.team4.store.supplier.entity.SupplierEntity;

import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String name,
        String address,
        String phone,
        String email,
        boolean isActive) {

    public static SupplierResponse from(SupplierEntity supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getAddress(),
                supplier.getPhone(),
                supplier.getEmail(),
                supplier.isActive());
    }
}
