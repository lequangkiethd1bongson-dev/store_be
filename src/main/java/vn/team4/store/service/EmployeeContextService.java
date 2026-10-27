package vn.team4.store.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import vn.team4.store.model.EmployeeContext;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class EmployeeContextService {

    private final JdbcTemplate jdbcTemplate;

    public EmployeeContextService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public EmployeeContext requireAuthenticatedEmployee(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ");
        }

        UUID employeeId;
        try {
            employeeId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Subject JWT không hợp lệ");
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, role::text AS role, is_active AS active, store_id, warehouse_id " +
                "FROM public.employees WHERE id = ?",
                employeeId
        );

        if (rows.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản chưa có hồ sơ nhân viên. Vui lòng liên hệ Admin."
            );
        }

        Map<String, Object> row = rows.get(0);
        String role = String.valueOf(row.get("role"));
        boolean active = Boolean.TRUE.equals(row.get("active"));
        if (!active) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản chưa được Admin kích hoạt hoặc cấp quyền."
            );
        }

        UUID storeId = row.get("store_id") == null ? null : UUID.fromString(String.valueOf(row.get("store_id")));
        UUID warehouseId = row.get("warehouse_id") == null ? null : UUID.fromString(String.valueOf(row.get("warehouse_id")));

        return new EmployeeContext(employeeId, role, storeId, warehouseId, active);
    }

    public EmployeeContext requireStoreEmployee(Authentication authentication) {
        EmployeeContext employee = requireAuthenticatedEmployee(authentication);
        if (employee.storeId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nhân viên chưa được gán cửa hàng");
        }
        if (!Set.of("store_manager", "sales").contains(employee.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập dữ liệu cửa hàng");
        }
        return employee;
    }

    public EmployeeContext requireWarehouseEmployee(Authentication authentication) {
        EmployeeContext employee = requireAuthenticatedEmployee(authentication);
        if (employee.warehouseId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nhân viên chưa được gán kho");
        }
        if (!Set.of("kho_manager", "kho").contains(employee.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác kho");
        }
        return employee;
    }

    public EmployeeContext requireStoreManager(Authentication authentication) {
        EmployeeContext employee = requireAuthenticatedEmployee(authentication);
        if (!"store_manager".equals(employee.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ quản lý cửa hàng mới được thực hiện thao tác này");
        }
        if (employee.storeId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản quản lý chưa được gán cửa hàng");
        }
        return employee;
    }

    public EmployeeContext requireWarehouseManagerOrStaff(Authentication authentication) {
        EmployeeContext employee = requireAuthenticatedEmployee(authentication);
        if (!Set.of("kho_manager", "kho").contains(employee.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ nhân viên kho mới được thực hiện thao tác này");
        }
        if (employee.warehouseId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản kho chưa được gán kho");
        }
        return employee;
    }
}
