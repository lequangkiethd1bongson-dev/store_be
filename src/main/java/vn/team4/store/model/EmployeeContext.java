package vn.team4.store.model;

import java.util.UUID;

public record EmployeeContext(
        UUID id,
        String role,
        UUID storeId,
        UUID warehouseId,
        boolean active
) {
}
