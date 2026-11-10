package vn.team4.store.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import vn.team4.store.model.EmployeeContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class EmployeeContextServiceTest {

    private final EmployeeContextService service =
            new EmployeeContextService(mock(JdbcTemplate.class));

    @Test
    void administratorsCanResolveAnyStoreAndWarehouseWithoutAssignments() {
        EmployeeContext admin = new EmployeeContext(UUID.randomUUID(), "admin", null, null, true);
        UUID storeId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();

        assertEquals(storeId, service.resolveStoreScope(admin, storeId));
        assertEquals(warehouseId, service.resolveWarehouseScope(admin, warehouseId));
        assertNull(service.resolveStoreScope(admin, null));
        assertNull(service.resolveWarehouseScope(admin, null));
        assertDoesNotThrow(() -> service.requireStoreAccess(admin, storeId));
        assertDoesNotThrow(() -> service.requireWarehouseAccess(admin, warehouseId));
    }

    @Test
    void storeRolesRemainLimitedToTheirAssignedStore() {
        UUID assignedStoreId = UUID.randomUUID();
        UUID otherStoreId = UUID.randomUUID();

        for (String role : new String[] {"store_manager", "sales"}) {
            EmployeeContext employee =
                    new EmployeeContext(UUID.randomUUID(), role, assignedStoreId, null, true);

            assertEquals(assignedStoreId, service.resolveStoreScope(employee, null));
            assertEquals(assignedStoreId, service.resolveStoreScope(employee, assignedStoreId));
            assertThrows(
                    ResponseStatusException.class,
                    () -> service.resolveStoreScope(employee, otherStoreId));
        }
    }

    @Test
    void warehouseRolesRemainLimitedToTheirAssignedWarehouse() {
        UUID assignedWarehouseId = UUID.randomUUID();
        UUID otherWarehouseId = UUID.randomUUID();

        for (String role : new String[] {"kho_manager", "kho"}) {
            EmployeeContext employee =
                    new EmployeeContext(UUID.randomUUID(), role, null, assignedWarehouseId, true);

            assertEquals(assignedWarehouseId, service.resolveWarehouseScope(employee, null));
            assertEquals(assignedWarehouseId, service.resolveWarehouseScope(employee, assignedWarehouseId));
            assertThrows(
                    ResponseStatusException.class,
                    () -> service.resolveWarehouseScope(employee, otherWarehouseId));
        }
    }
}
