package vn.team4.store.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JdbcTemplate jdbcTemplate;

    public AuthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        UUID employeeId = UUID.fromString(authentication.getName());
        return jdbcTemplate.queryForObject("""
                SELECT id::text AS id,
                       full_name AS name,
                       COALESCE(email, '') AS email,
                       role::text AS role,
                       is_active AS "isActive",
                       store_id::text AS "storeId",
                       warehouse_id::text AS "warehouseId"
                FROM public.employees
                WHERE id = ?
                """, (rs, rowNum) -> {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("id", rs.getString("id"));
            profile.put("name", rs.getString("name"));
            profile.put("email", rs.getString("email"));
            profile.put("role", rs.getString("role"));
            profile.put("isActive", rs.getBoolean("isActive"));
            profile.put("storeId", rs.getString("storeId"));
            profile.put("warehouseId", rs.getString("warehouseId"));
            return profile;
        }, employeeId);
    }
}
