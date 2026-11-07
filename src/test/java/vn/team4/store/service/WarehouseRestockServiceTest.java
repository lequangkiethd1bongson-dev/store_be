package vn.team4.store.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import vn.team4.store.model.EmployeeContext;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WarehouseRestockServiceTest {

    @Test
    void rejectsApprovedRequestAfterFailedExportWithoutChangingStock() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        EmployeeContextService employeeContextService = mock(EmployeeContextService.class);
        WarehouseRestockService service = new WarehouseRestockService(jdbcTemplate, employeeContextService);

        UUID requestId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        String reason = "Huỷ phiếu vì kho không đủ tồn";
        Authentication authentication = mock(Authentication.class);

        when(employeeContextService.requireWarehouseManager(authentication))
                .thenReturn(new EmployeeContext(employeeId, "KHO_MANAGER", null, warehouseId, true));
        when(jdbcTemplate.queryForMap(anyString(), eq(requestId)))
                .thenReturn(Map.of("warehouse_id", warehouseId, "status", "approved"));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(requestId)))
                .thenReturn(0);

        WarehouseRestockService.RejectResult result =
                service.rejectRequest(authentication, requestId, reason);

        assertEquals(requestId, result.requestId());
        assertEquals("rejected", result.status());
        verify(jdbcTemplate).update(
                "UPDATE public.restock_requests SET status = 'rejected', reject_reason = ? WHERE id = ?",
                reason,
                requestId
        );
    }
}
