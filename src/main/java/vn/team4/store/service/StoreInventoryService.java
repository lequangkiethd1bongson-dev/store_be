package vn.team4.store.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import vn.team4.store.model.EmployeeContext;

import java.math.BigDecimal;
import java.util.List;

@Service
public class StoreInventoryService {

    private final JdbcTemplate jdbcTemplate;
    private final EmployeeContextService employeeContextService;

    public StoreInventoryService(JdbcTemplate jdbcTemplate, EmployeeContextService employeeContextService) {
        this.jdbcTemplate = jdbcTemplate;
        this.employeeContextService = employeeContextService;
    }

    public List<StoreProductResponse> getProductsForCurrentStore(Authentication authentication) {
        EmployeeContext employee = employeeContextService.requireStoreEmployee(authentication);

        String sql = """
                SELECT p.id, p.name, p.barcode, p.image_url AS image_url,
                       p.price,
                       COALESCE(ss.quantity, 0) AS store_stock_quantity,
                       COALESCE(ss.min_quantity, 5) AS min_quantity,
                       CASE
                           WHEN COALESCE(ss.quantity, 0) <= COALESCE(ss.min_quantity, 5)
                               THEN true ELSE false
                       END AS low_stock
                FROM public.products p
                LEFT JOIN public.store_stock ss
                    ON ss.product_id = p.id AND ss.store_id = ?
                WHERE p.is_active = true
                ORDER BY p.name ASC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new StoreProductResponse(
                java.util.UUID.fromString(rs.getString("id")),
                rs.getString("name"),
                rs.getString("barcode"),
                rs.getString("image_url"),
                rs.getBigDecimal("price"),
                rs.getInt("store_stock_quantity"),
                rs.getInt("min_quantity"),
                rs.getBoolean("low_stock")
        ), employee.storeId());
    }

    public record StoreProductResponse(
            java.util.UUID id,
            String name,
            String barcode,
            String imageUrl,
            BigDecimal price,
            int storeStockQuantity,
            int minQuantity,
            boolean lowStock
    ) {
    }
}
