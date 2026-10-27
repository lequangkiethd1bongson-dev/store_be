package vn.team4.store.service;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.team4.store.model.EmployeeContext;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class StoreRestockService {

    private final JdbcTemplate jdbcTemplate;
    private final EmployeeContextService employeeContextService;

    public StoreRestockService(JdbcTemplate jdbcTemplate, EmployeeContextService employeeContextService) {
        this.jdbcTemplate = jdbcTemplate;
        this.employeeContextService = employeeContextService;
    }

    @Transactional
    public RestockRequestCreateResponse createRequest(Authentication authentication, RestockRequestCreateRequest request) {
        EmployeeContext employee = employeeContextService.requireStoreManager(authentication);

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload không được để trống");
        }
        if (request.warehouseId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "warehouseId là bắt buộc");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách sản phẩm yêu cầu không được rỗng");
        }

        Integer warehouseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.warehouses WHERE id = ? AND COALESCE(is_active, true) = true",
                Integer.class,
                request.warehouseId()
        );
        if (warehouseCount == null || warehouseCount == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy kho hợp lệ để tạo yêu cầu");
        }

        Set<UUID> seenProducts = new HashSet<>();
        for (RestockRequestLineRequest item : request.items()) {
            if (item == null || item.productId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mỗi sản phẩm cần có productId");
            }
            if (item.requestedQuantity() == null || item.requestedQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng yêu cầu phải là số nguyên dương");
            }
            if (!seenProducts.add(item.productId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mỗi sản phẩm chỉ được xuất hiện một lần");
            }

            Integer productCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM public.products WHERE id = ? AND is_active = true",
                    Integer.class,
                    item.productId()
            );
            if (productCount == null || productCount == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm không tồn tại hoặc đã ngừng hoạt động");
            }
        }

        UUID requestId = UUID.randomUUID();
        String note = request.note() == null ? null : request.note().trim();

        jdbcTemplate.update(
                "INSERT INTO public.restock_requests (id, store_id, warehouse_id, status, created_by, note, created_at) VALUES (?, ?, ?, 'pending', ?, ?, now())",
                requestId,
                employee.storeId(),
                request.warehouseId(),
                employee.id(),
                note
        );

        for (RestockRequestLineRequest item : request.items()) {
            jdbcTemplate.update(
                    "INSERT INTO public.restock_request_items (request_id, product_id, requested_quantity, approved_quantity) VALUES (?, ?, ?, NULL)",
                    requestId,
                    item.productId(),
                    item.requestedQuantity()
            );
        }

        return new RestockRequestCreateResponse(requestId, "pending", employee.storeId(), request.warehouseId(), note);
    }

    public List<RestockRequestSummaryResponse> listRequests(
            Authentication authentication,
            String status
    ) {
        EmployeeContext employee = employeeContextService.requireStoreManager(authentication);

        StringBuilder sql = new StringBuilder("""
                SELECT r.id,
                       r.warehouse_id,
                       w.name AS warehouse_name,
                       r.status,
                       r.note,
                       r.created_at,
                       COUNT(i.product_id)::int AS item_count,
                       e.id AS export_id,
                       e.status::text AS export_status
                FROM public.restock_requests r
                JOIN public.warehouses w ON w.id = r.warehouse_id
                LEFT JOIN public.restock_request_items i ON i.request_id = r.id
                LEFT JOIN public.stock_exports e ON e.request_id = r.id
                WHERE r.store_id = ?
                """);
        List<Object> params = new java.util.ArrayList<>();
        params.add(employee.storeId());

        if (status != null && !status.isBlank()) {
            String normalizedStatus = status.trim().toLowerCase(java.util.Locale.ROOT);
            if (!Set.of("pending", "approved", "rejected", "exported", "received")
                    .contains(normalizedStatus)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái phiếu không hợp lệ");
            }
            sql.append(" AND r.status::text = ? ");
            params.add(normalizedStatus);
        }

        sql.append("""
                 GROUP BY r.id, w.name, e.id, e.status
                 ORDER BY r.created_at DESC
                """);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new RestockRequestSummaryResponse(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("warehouse_id")),
                rs.getString("warehouse_name"),
                rs.getString("status"),
                rs.getString("note"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getInt("item_count"),
                rs.getString("export_id") == null ? null : UUID.fromString(rs.getString("export_id")),
                rs.getString("export_status")
        ), params.toArray());
    }

    public RestockRequestDetailResponse getRequestDetail(Authentication authentication, UUID requestId) {
        EmployeeContext employee = employeeContextService.requireStoreManager(authentication);

        String sql = """
                SELECT r.id,
                       r.store_id,
                       r.warehouse_id,
                       w.name AS warehouse_name,
                       r.status,
                       r.note,
                       r.reject_reason,
                       r.created_by,
                       r.approved_by,
                       r.created_at,
                       r.approved_at
                FROM public.restock_requests r
                JOIN public.warehouses w ON w.id = r.warehouse_id
                WHERE r.id = ? AND r.store_id = ?
                """;

        Map<String, Object> row;
        try {
            row = jdbcTemplate.queryForMap(sql, requestId, employee.storeId());
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu yêu cầu");
        }

        List<RestockRequestItemDetail> items = jdbcTemplate.query(
                """
                SELECT i.product_id,
                       p.name AS product_name,
                       i.requested_quantity,
                       i.approved_quantity
                FROM public.restock_request_items i
                JOIN public.products p ON p.id = i.product_id
                WHERE i.request_id = ?
                ORDER BY p.name ASC
                """,
                (rs, rowNum) -> new RestockRequestItemDetail(
                        UUID.fromString(rs.getString("product_id")),
                        rs.getString("product_name"),
                        rs.getInt("requested_quantity"),
                        (Integer) rs.getObject("approved_quantity")
                ),
                requestId
        );

        Map<String, Object> exportRow = null;
        try {
            exportRow = jdbcTemplate.queryForMap(
                    "SELECT id, status FROM public.stock_exports WHERE request_id = ?",
                    requestId
            );
        } catch (EmptyResultDataAccessException ignored) {
            // no export yet
        }

        return new RestockRequestDetailResponse(
                UUID.fromString(String.valueOf(row.get("id"))),
                UUID.fromString(String.valueOf(row.get("store_id"))),
                UUID.fromString(String.valueOf(row.get("warehouse_id"))),
                String.valueOf(row.get("warehouse_name")),
                String.valueOf(row.get("status")),
                row.get("note") == null ? null : String.valueOf(row.get("note")),
                row.get("reject_reason") == null ? null : String.valueOf(row.get("reject_reason")),
                row.get("created_at") == null ? null : (OffsetDateTime) row.get("created_at"),
                row.get("approved_at") == null ? null : (OffsetDateTime) row.get("approved_at"),
                row.get("approved_by") == null ? null : UUID.fromString(String.valueOf(row.get("approved_by"))),
                items,
                exportRow == null ? null : UUID.fromString(String.valueOf(exportRow.get("id"))),
                exportRow == null ? null : String.valueOf(exportRow.get("status"))
        );
    }

    @Transactional
    public ReceiptConfirmationResponse confirmReceipt(Authentication authentication, UUID exportId) {
        EmployeeContext employee = employeeContextService.requireStoreManager(authentication);

        Map<String, Object> exportRow;
        try {
            exportRow = jdbcTemplate.queryForMap(
                    "SELECT id, store_id, status FROM public.stock_exports WHERE id = ? FOR UPDATE",
                    exportId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu xuất");
        }

        UUID exportStoreId = UUID.fromString(String.valueOf(exportRow.get("store_id")));
        if (!exportStoreId.equals(employee.storeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xác nhận phiếu xuất của cửa hàng khác");
        }
        if (!"shipping".equals(String.valueOf(exportRow.get("status")))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu xuất đã được xác nhận hoặc không ở trạng thái shipping");
        }

        List<Map<String, Object>> items = jdbcTemplate.queryForList(
                "SELECT product_id, quantity FROM public.stock_export_items WHERE export_id = ?",
                exportId
        );
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu xuất không có sản phẩm để nhận");
        }

        for (Map<String, Object> item : items) {
            UUID productId = UUID.fromString(String.valueOf(item.get("product_id")));
            int quantity = ((Number) item.get("quantity")).intValue();
            jdbcTemplate.update(
                    """
                    INSERT INTO public.store_stock (store_id, product_id, quantity, min_quantity, updated_at)
                    VALUES (?, ?, ?, 5, now())
                    ON CONFLICT (store_id, product_id)
                    DO UPDATE SET quantity = public.store_stock.quantity + EXCLUDED.quantity,
                                  updated_at = now()
                    """,
                    employee.storeId(),
                    productId,
                    quantity
            );

            jdbcTemplate.update(
                    """
                    UPDATE public.stock_alerts
                       SET status = 'resolved', resolved_at = now()
                     WHERE store_id = ?
                       AND product_id = ?
                       AND status = 'open'
                       AND EXISTS (
                           SELECT 1 FROM public.store_stock s
                           WHERE s.store_id = public.stock_alerts.store_id
                             AND s.product_id = public.stock_alerts.product_id
                             AND s.quantity > s.min_quantity
                       )
                    """,
                    employee.storeId(),
                    productId
            );
        }

        int updatedExports = jdbcTemplate.update(
                "UPDATE public.stock_exports SET status = 'delivered', received_by = ?, received_at = now() WHERE id = ? AND status = 'shipping'",
                employee.id(),
                exportId
        );
        if (updatedExports != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu xuất không còn ở trạng thái shipping");
        }

        int updatedRequests = jdbcTemplate.update(
                "UPDATE public.restock_requests SET status = 'received' WHERE id = (SELECT request_id FROM public.stock_exports WHERE id = ?) AND status = 'exported'",
                exportId
        );
        if (updatedRequests != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu yêu cầu không còn ở trạng thái exported");
        }

        return new ReceiptConfirmationResponse(exportId, "delivered");
    }

    public record RestockRequestCreateRequest(UUID warehouseId, String note, List<RestockRequestLineRequest> items) {
    }

    public record RestockRequestLineRequest(UUID productId, Integer requestedQuantity) {
    }

    public record RestockRequestCreateResponse(UUID id, String status, UUID storeId, UUID warehouseId, String note) {
    }

    public record RestockRequestSummaryResponse(
            UUID id,
            UUID warehouseId,
            String warehouseName,
            String status,
            String note,
            OffsetDateTime createdAt,
            int itemCount,
            UUID exportId,
            String exportStatus
    ) {
    }

    public record RestockRequestItemDetail(
            UUID productId,
            String productName,
            int requestedQuantity,
            Integer approvedQuantity
    ) {
    }

    public record RestockRequestDetailResponse(
            UUID id,
            UUID storeId,
            UUID warehouseId,
            String warehouseName,
            String status,
            String note,
            String rejectReason,
            OffsetDateTime createdAt,
            OffsetDateTime approvedAt,
            UUID approvedBy,
            List<RestockRequestItemDetail> items,
            UUID exportId,
            String exportStatus
    ) {
    }

    public record ReceiptConfirmationResponse(UUID exportId, String status) {
    }
}
