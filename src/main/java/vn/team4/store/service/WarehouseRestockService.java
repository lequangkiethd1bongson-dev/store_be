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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WarehouseRestockService {

    private final JdbcTemplate jdbcTemplate;
    private final EmployeeContextService employeeContextService;

    public WarehouseRestockService(JdbcTemplate jdbcTemplate, EmployeeContextService employeeContextService) {
        this.jdbcTemplate = jdbcTemplate;
        this.employeeContextService = employeeContextService;
    }

    public List<RestockRequestSummaryResponse> listRequests(Authentication authentication, String status) {
        EmployeeContext employee = employeeContextService.requireWarehouseEmployee(authentication);

        StringBuilder sql = new StringBuilder("""
                SELECT r.id,
                       r.store_id,
                       s.name AS store_name,
                       r.status,
                       r.note,
                       r.created_at,
                       COUNT(i.product_id)::int AS item_count
                FROM public.restock_requests r
                JOIN public.stores s ON s.id = r.store_id
                LEFT JOIN public.restock_request_items i ON i.request_id = r.id
                WHERE r.warehouse_id = ?
                """);
        List<Object> params = new java.util.ArrayList<>();
        params.add(employee.warehouseId());

        if (status != null && !status.isBlank()) {
            String normalizedStatus = status.trim().toLowerCase(java.util.Locale.ROOT);
            if (!List.of("pending", "approved", "rejected", "exported", "received")
                    .contains(normalizedStatus)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái phiếu không hợp lệ");
            }
            sql.append(" AND r.status::text = ? ");
            params.add(normalizedStatus);
        }
        sql.append(" GROUP BY r.id, s.name ORDER BY r.created_at DESC");

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new RestockRequestSummaryResponse(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("store_id")),
                        rs.getString("store_name"),
                        rs.getString("status"),
                        rs.getString("note"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getInt("item_count")
                ),
                params.toArray()
        );
    }

    public RestockRequestDetailResponse getRequestDetail(Authentication authentication, UUID requestId) {
        EmployeeContext employee = employeeContextService.requireWarehouseEmployee(authentication);

        Map<String, Object> row;
        try {
            row = jdbcTemplate.queryForMap(
                    """
                    SELECT r.id,
                           r.store_id,
                           s.name AS store_name,
                           r.status,
                           r.note,
                           r.reject_reason,
                           r.created_by,
                           r.approved_by,
                           r.created_at,
                           r.approved_at,
                           r.warehouse_id
                    FROM public.restock_requests r
                    JOIN public.stores s ON s.id = r.store_id
                    WHERE r.id = ? AND r.warehouse_id = ?
                    """,
                    requestId,
                    employee.warehouseId()
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu thuộc kho hiện tại");
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
            // no export
        }

        return new RestockRequestDetailResponse(
                UUID.fromString(String.valueOf(row.get("id"))),
                UUID.fromString(String.valueOf(row.get("store_id"))),
                String.valueOf(row.get("store_name")),
                UUID.fromString(String.valueOf(row.get("warehouse_id"))),
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
    public ApproveResult approveRequest(Authentication authentication, UUID requestId) {
        EmployeeContext employee = employeeContextService.requireWarehouseManagerOrStaff(authentication);

        Map<String, Object> requestRow;
        try {
            requestRow = jdbcTemplate.queryForMap(
                    "SELECT id, status, warehouse_id FROM public.restock_requests WHERE id = ? FOR UPDATE",
                    requestId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu yêu cầu");
        }

        UUID requestWarehouseId = UUID.fromString(String.valueOf(requestRow.get("warehouse_id")));
        if (!requestWarehouseId.equals(employee.warehouseId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền duyệt phiếu của kho khác");
        }
        if (!"pending".equals(String.valueOf(requestRow.get("status")))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu không ở trạng thái pending");
        }

        int rowsUpdated = jdbcTemplate.update(
                "UPDATE public.restock_request_items SET approved_quantity = requested_quantity WHERE request_id = ?",
                requestId
        );
        if (rowsUpdated == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phiếu yêu cầu không có sản phẩm để duyệt");
        }

        jdbcTemplate.update(
                "UPDATE public.restock_requests SET status = 'approved', approved_by = ?, approved_at = now() WHERE id = ?",
                employee.id(),
                requestId
        );

        return new ApproveResult(requestId, "approved");
    }

    @Transactional
    public RejectResult rejectRequest(Authentication authentication, UUID requestId, String reason) {
        EmployeeContext employee = employeeContextService.requireWarehouseManagerOrStaff(authentication);

        Map<String, Object> requestRow;
        try {
            requestRow = jdbcTemplate.queryForMap(
                    "SELECT id, status, warehouse_id FROM public.restock_requests WHERE id = ? FOR UPDATE",
                    requestId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu yêu cầu");
        }

        UUID requestWarehouseId = UUID.fromString(String.valueOf(requestRow.get("warehouse_id")));
        if (!requestWarehouseId.equals(employee.warehouseId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền từ chối phiếu của kho khác");
        }
        if (!"pending".equals(String.valueOf(requestRow.get("status")))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu không ở trạng thái pending");
        }
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lý do từ chối là bắt buộc");
        }

        jdbcTemplate.update(
                "UPDATE public.restock_requests SET status = 'rejected', reject_reason = ?, approved_by = ?, approved_at = now() WHERE id = ?",
                reason,
                employee.id(),
                requestId
        );

        return new RejectResult(requestId, "rejected");
    }

    @Transactional
    public ExportResult exportRequest(Authentication authentication, UUID requestId) {
        EmployeeContext employee = employeeContextService.requireWarehouseManagerOrStaff(authentication);

        Map<String, Object> requestRow;
        try {
            requestRow = jdbcTemplate.queryForMap(
                    "SELECT id, status, warehouse_id, store_id FROM public.restock_requests WHERE id = ? FOR UPDATE",
                    requestId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu yêu cầu");
        }

        UUID requestWarehouseId = UUID.fromString(String.valueOf(requestRow.get("warehouse_id")));
        if (!requestWarehouseId.equals(employee.warehouseId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xuất kho cho phiếu của kho khác");
        }
        if (!"approved".equals(String.valueOf(requestRow.get("status")))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể xuất kho cho phiếu đã duyệt");
        }

        Integer existingExportCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.stock_exports WHERE request_id = ?",
                Integer.class,
                requestId
        );
        if (existingExportCount != null && existingExportCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu yêu cầu đã có phiếu xuất, không được xuất lại");
        }

        List<Map<String, Object>> approvedItems = jdbcTemplate.queryForList(
                "SELECT product_id, approved_quantity FROM public.restock_request_items WHERE request_id = ? AND approved_quantity IS NOT NULL AND approved_quantity > 0 ORDER BY product_id",
                requestId
        );
        if (approvedItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không có sản phẩm nào được duyệt để xuất kho");
        }

        for (Map<String, Object> item : approvedItems) {
            UUID productId = UUID.fromString(String.valueOf(item.get("product_id")));
            int approvedQuantity = ((Number) item.get("approved_quantity")).intValue();
            Integer currentQuantity;
            try {
                currentQuantity = jdbcTemplate.queryForObject(
                        "SELECT quantity FROM public.warehouse_stock WHERE warehouse_id = ? AND product_id = ? FOR UPDATE",
                        Integer.class,
                        employee.warehouseId(),
                        productId
                );
            } catch (EmptyResultDataAccessException ex) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Kho không có tồn cho sản phẩm: " + productId
                );
            }
            if (currentQuantity == null || currentQuantity < approvedQuantity) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Kho không đủ hàng cho sản phẩm: " + productId);
            }
        }

        UUID exportId = UUID.randomUUID();
        UUID storeId = UUID.fromString(String.valueOf(requestRow.get("store_id")));
        jdbcTemplate.update(
                "INSERT INTO public.stock_exports (id, request_id, warehouse_id, store_id, status, created_by, created_at) VALUES (?, ?, ?, ?, 'shipping', ?, now())",
                exportId,
                requestId,
                employee.warehouseId(),
                storeId,
                employee.id()
        );

        for (Map<String, Object> item : approvedItems) {
            UUID productId = UUID.fromString(String.valueOf(item.get("product_id")));
            int approvedQuantity = ((Number) item.get("approved_quantity")).intValue();

            int updated = jdbcTemplate.update(
                    "UPDATE public.warehouse_stock SET quantity = quantity - ?, updated_at = now() WHERE warehouse_id = ? AND product_id = ? AND quantity >= ?",
                    approvedQuantity,
                    employee.warehouseId(),
                    productId,
                    approvedQuantity
            );
            if (updated != 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Tồn kho đã thay đổi hoặc không đủ hàng cho sản phẩm: " + productId
                );
            }

            jdbcTemplate.update(
                    "INSERT INTO public.stock_export_items (export_id, product_id, quantity) VALUES (?, ?, ?)",
                    exportId,
                    productId,
                    approvedQuantity
            );
        }

        jdbcTemplate.update(
                "UPDATE public.restock_requests SET status = 'exported' WHERE id = ?",
                requestId
        );

        return new ExportResult(exportId, "shipping");
    }

    public record RestockRequestSummaryResponse(
            UUID id,
            UUID storeId,
            String storeName,
            String status,
            String note,
            OffsetDateTime createdAt,
            int itemCount
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
            String storeName,
            UUID warehouseId,
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

    public record ApproveResult(UUID requestId, String status) {
    }

    public record RejectResult(UUID requestId, String status) {
    }

    public record ExportResult(UUID exportId, String status) {
    }
}
