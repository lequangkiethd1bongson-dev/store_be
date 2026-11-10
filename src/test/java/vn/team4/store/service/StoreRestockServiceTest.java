package vn.team4.store.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import vn.team4.store.model.EmployeeContext;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreRestockServiceTest {

    @Test
    void administratorWithoutStoreAssignmentCanCreateRequestForSelectedStoreAndWarehouse() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        EmployeeContextService employeeContextService = mock(EmployeeContextService.class);
        StoreRestockService service = new StoreRestockService(jdbcTemplate, employeeContextService);
        Authentication authentication = mock(Authentication.class);

        UUID adminId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        EmployeeContext admin = new EmployeeContext(adminId, "admin", null, null, true);

        when(employeeContextService.requireStoreManager(authentication)).thenReturn(admin);
        when(employeeContextService.isAdmin(admin)).thenReturn(true);
        when(employeeContextService.resolveStoreScope(admin, storeId)).thenReturn(storeId);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(storeId))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(warehouseId))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(productId))).thenReturn(1);

        StoreRestockService.RestockRequestCreateRequest request =
                new StoreRestockService.RestockRequestCreateRequest(
                        warehouseId,
                        "restock",
                        List.of(new StoreRestockService.RestockRequestLineRequest(productId, 3)),
                        storeId);

        StoreRestockService.RestockRequestCreateResponse response =
                service.createRequest(authentication, request);

        assertEquals(storeId, response.storeId());
        assertEquals(warehouseId, response.warehouseId());
        verify(jdbcTemplate).update(
                org.mockito.ArgumentMatchers.startsWith("INSERT INTO public.restock_requests"),
                org.mockito.ArgumentMatchers.any(UUID.class),
                eq(storeId),
                eq(warehouseId),
                eq(adminId),
                eq("restock"));
    }
}
