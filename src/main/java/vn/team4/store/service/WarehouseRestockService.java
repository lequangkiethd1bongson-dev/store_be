package vn.team4.store.service;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.team4.store.model.EmployeeContext;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
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

    public List<RestockRequestSummaryResponse> listRequests(Authentication authentication, String status, Integer page, Integer size) {
        EmployeeContext employee = employeeContextService.requireWarehouseManager(authentication);

        StringBuilder sql = new StringBuilder("""
                SELECT r.id,
                       r.store_id,
                       s.name AS store_name,
                       r.warehouse_id,
                       w.name AS warehouse_name,
                       r.status,
                       r.note,
                       r.created_by,
                       e.full_name AS created_by_name,
                       r.created_at,
                       r.approved_at,
                       r.reject_reason,
                       COUNT(i.product_id)::int AS item_count,
                       COALESCE(SUM(i.requested_quantity), 0)::int AS total_requested_quantity
                FROM public.restock_requests r
                JOIN public.stores s ON s.id = r.store_id
                JOIN public.warehouses w ON w.id = r.warehouse_id
                LEFT JOIN public.employees e ON e.id = r.created_by
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
        sql.append(" GROUP BY r.id, s.name, w.name, r.store_id, r.warehouse_id, r.status, r.note, r.created_by, e.full_name, r.created_at, r.approved_at, r.reject_reason ORDER BY r.created_at DESC");

        if (page != null || size != null) {
            int pageNumber = page == null ? 0 : Math.max(page, 0);
            int pageSize = size == null ? 20 : Math.max(size, 1);
            sql.append(" LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add(pageNumber * pageSize);
        }

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new RestockRequestSummaryResponse(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("store_id")),
                        rs.getString("store_name"),
                        UUID.fromString(rs.getString("warehouse_id")),
                        rs.getString("warehouse_name"),
                        rs.getString("status"),
                        rs.getString("note"),
                        toOffsetDateTime(rs.getTimestamp("created_at")),
                        rs.getString("created_by") == null ? null : UUID.fromString(rs.getString("created_by")),
                        rs.getString("created_by_name"),
                        rs.getInt("item_count"),
                        rs.getInt("total_requested_quantity"),
                        toOffsetDateTime(rs.getTimestamp("approved_at")),
                        rs.getString("reject_reason")
                ),
                params.toArray()
        );
    }

    public RestockRequestDetailResponse getRequestDetail(Authentication authentication, UUID requestId) {
        EmployeeContext employee = employeeContextService.requireWarehouseManager(authentication);

        Map<String, Object> row;
        try {
            row = jdbcTemplate.queryForMap(
                    """
                    SELECT r.id,
                           r.store_id,
                           s.name AS store_name,
                           r.warehouse_id,
                           w.name AS warehouse_name,
                           r.status,
                           r.note,
                           r.reject_reason,
                           r.created_by,
                           ec.full_name AS created_by_name,
                           r.approved_by,
                           ea.full_name AS approved_by_name,
                           ex.id AS export_id,
                           ex.status AS export_status,
                           ex.created_at AS export_created_at,
                           ex.received_at,
                           er.full_name AS received_by_name,
                           ex.created_by AS export_created_by_id,
                           ee.full_name AS export_created_by_name,
                           r.created_at,
                           r.approved_at
                    FROM public.restock_requests r
                    JOIN public.stores s ON s.id = r.store_id
                    JOIN public.warehouses w ON w.id = r.warehouse_id
                    LEFT JOIN public.employees ec ON ec.id = r.created_by
                    LEFT JOIN public.employees ea ON ea.id = r.approved_by
                    LEFT JOIN public.stock_exports ex ON ex.request_id = r.id
                    LEFT JOIN public.employees ee ON ee.id = ex.created_by
                    LEFT JOIN public.employees er ON er.id = ex.received_by
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
                       p.barcode,
                       p.image_url,
                       p.package_size AS unit,
                       p.is_active,
                       i.requested_quantity,
                       i.approved_quantity,
                       COALESCE(ws.quantity, 0) AS warehouse_stock_quantity
                FROM public.restock_request_items i
                JOIN public.products p ON p.id = i.product_id
                LEFT JOIN public.warehouse_stock ws ON ws.product_id = i.product_id AND ws.warehouse_id = ?
                WHERE i.request_id = ?
                ORDER BY p.name ASC
                """,
                (rs, rowNum) -> new RestockRequestItemDetail(
                        UUID.fromString(rs.getString("product_id")),
                        rs.getString("product_name"),
                        rs.getString("barcode"),
                        rs.getString("image_url"),
                        rs.getString("unit"),
                        rs.getInt("requested_quantity"),
                        (Integer) rs.getObject("approved_quantity"),
                        rs.getInt("warehouse_stock_quantity"),
                        rs.getBoolean("is_active")
                ),
                employee.warehouseId(),
                requestId
        );

        return new RestockRequestDetailResponse(
                UUID.fromString(String.valueOf(row.get("id"))),
                UUID.fromString(String.valueOf(row.get("store_id"))),
                String.valueOf(row.get("store_name")),
                UUID.fromString(String.valueOf(row.get("warehouse_id"))),
                String.valueOf(row.get("warehouse_name")),
                String.valueOf(row.get("status")),
                row.get("note") == null ? null : String.valueOf(row.get("note")),
                row.get("reject_reason") == null ? null : String.valueOf(row.get("reject_reason")),
                row.get("created_by") == null ? null : UUID.fromString(String.valueOf(row.get("created_by"))),
                row.get("created_by_name") == null ? null : String.valueOf(row.get("created_by_name")),
                row.get("approved_by") == null ? null : UUID.fromString(String.valueOf(row.get("approved_by"))),
                row.get("approved_by_name") == null ? null : String.valueOf(row.get("approved_by_name")),
                row.get("export_created_by_name") == null ? null : String.valueOf(row.get("export_created_by_name")),
                row.get("received_by_name") == null ? null : String.valueOf(row.get("received_by_name")),
                toOffsetDateTime(row.get("created_at")),
                toOffsetDateTime(row.get("approved_at")),
                items,
                row.get("export_id") == null ? null : UUID.fromString(String.valueOf(row.get("export_id"))),
                row.get("export_status") == null ? null : String.valueOf(row.get("export_status")),
                toOffsetDateTime(row.get("export_created_at")),
                toOffsetDateTime(row.get("received_at"))
        );
    }

    @Transactional
    public ApproveResult approveRequest(Authentication authentication, UUID requestId, ApproveRequest request) {
        EmployeeContext employee = employeeContextService.requireWarehouseManager(authentication);

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

        if (request == null || request.items() == null || request.items().isEmpty()) {
            int rowsUpdated = jdbcTemplate.update(
                    "UPDATE public.restock_request_items SET approved_quantity = requested_quantity WHERE request_id = ?",
                    requestId
            );
            if (rowsUpdated == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phiếu yêu cầu không có sản phẩm để duyệt");
            }
        } else {
            for (ApproveItem item : request.items()) {
                if (item == null || item.productId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mỗi sản phẩm cần có productId");
                }
                if (item.approvedQuantity() == null || item.approvedQuantity() < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng duyệt phải là số nguyên không âm");
                }

                Integer requestedQuantity = jdbcTemplate.queryForObject(
                        "SELECT requested_quantity FROM public.restock_request_items WHERE request_id = ? AND product_id = ?",
                        Integer.class,
                        requestId,
                        item.productId()
                );
                if (requestedQuantity == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm không thuộc phiếu yêu cầu này");
                }
                if (item.approvedQuantity() > requestedQuantity) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng duyệt không được vượt quá số lượng yêu cầu");
                }

                jdbcTemplate.update(
                        "UPDATE public.restock_request_items SET approved_quantity = ? WHERE request_id = ? AND product_id = ?",
                        item.approvedQuantity(),
                        requestId,
                        item.productId()
                );
            }
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
        EmployeeContext employee = employeeContextService.requireWarehouseManager(authentication);

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
        String status = String.valueOf(requestRow.get("status"));
        if (!"pending".equals(status) && !"approved".equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể huỷ phiếu đang pending hoặc approved");
        }
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lý do từ chối hoặc huỷ phiếu là bắt buộc");
        }

        if ("approved".equals(status)) {
            Integer existingExportCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM public.stock_exports WHERE request_id = ?",
                    Integer.class,
                    requestId
            );
            if (existingExportCount != null && existingExportCount > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể huỷ phiếu sau khi đã tạo phiếu xuất");
            }

            jdbcTemplate.update(
                    "UPDATE public.restock_requests SET status = 'rejected', reject_reason = ? WHERE id = ?",
                    reason,
                    requestId
            );
        } else {
            jdbcTemplate.update(
                    "UPDATE public.restock_requests SET status = 'rejected', reject_reason = ?, approved_by = ?, approved_at = now() WHERE id = ?",
                    reason,
                    employee.id(),
                    requestId
            );
        }

        return new RejectResult(requestId, "rejected");
    }

    public ExportResult exportRequest(Authentication authentication, UUID requestId) {
        return exportRequest(authentication, requestId, null);
    }

    @Transactional
    public ExportResult exportRequest(Authentication authentication, UUID requestId, ExportRequest request) {
        EmployeeContext employee = employeeContextService.requireWarehouseManager(authentication);

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

        Map<String, Object> existingExport = null;
        try {
            existingExport = jdbcTemplate.queryForMap(
                    "SELECT id, status FROM public.stock_exports WHERE request_id = ? ORDER BY created_at DESC LIMIT 1",
                    requestId
            );
        } catch (EmptyResultDataAccessException ignored) {
            // no export yet
        }
        if (existingExport != null) {
            return new ExportResult(
                    UUID.fromString(String.valueOf(existingExport.get("id"))),
                    String.valueOf(existingExport.get("status"))
            );
        }

        List<Map<String, Object>> approvedItems = jdbcTemplate.queryForList(
                "SELECT product_id, approved_quantity FROM public.restock_request_items WHERE request_id = ? AND approved_quantity IS NOT NULL AND approved_quantity > 0 ORDER BY product_id",
                requestId
        );
        if (approvedItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không có sản phẩm nào được duyệt để xuất kho");
        }

        if (request == null || request.scannedItems() == null || request.scannedItems().isEmpty()) {
            throw new WarehouseExportValidationException(
                    "SCAN_INCOMPLETE",
                    "Chưa kiểm tra đủ số lượng sản phẩm.",
                    java.util.List.of()
            );
        }
        validateScannedItems(requestId, request.scannedItems(), approvedItems, employee.warehouseId());

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
                throw insufficientStock(productId, approvedQuantity, 0);
            }
            if (currentQuantity == null || currentQuantity < approvedQuantity) {
                throw insufficientStock(productId, approvedQuantity, currentQuantity == null ? 0 : currentQuantity);
            }
        }

        UUID exportId = UUID.randomUUID();
        UUID storeId = UUID.fromString(String.valueOf(requestRow.get("store_id")));
        try {
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
                    Integer available = jdbcTemplate.queryForObject(
                            "SELECT quantity FROM public.warehouse_stock WHERE warehouse_id = ? AND product_id = ?",
                            Integer.class,
                            employee.warehouseId(),
                            productId
                    );
                    throw insufficientStock(productId, approvedQuantity, available == null ? 0 : available);
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
        } catch (DataAccessException ex) {
            String message = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
            if (message != null && message.toUpperCase(Locale.ROOT).contains("INSUFFICIENT_STOCK")) {
                throw extractInsufficientStockError(message, approvedItems, employee.warehouseId());
            }
            throw ex;
        }

        return new ExportResult(exportId, requestId, "shipping", "exported", approvedItems.size(), approvedItems.stream().mapToInt(item -> ((Number) item.get("approved_quantity")).intValue()).sum(), java.util.List.<ExportItemSummary>of());
    }

    private void validateScannedItems(UUID requestId, List<ScanItem> scannedItems, List<Map<String, Object>> approvedItems, UUID warehouseId) {
        Map<String, Integer> scannedTotals = new java.util.HashMap<>();
        for (ScanItem scannedItem : scannedItems) {
            if (scannedItem == null || scannedItem.barcode() == null || scannedItem.barcode().isBlank()) {
                throw new WarehouseExportValidationException(
                        "BARCODE_NOT_FOUND",
                        "Barcode không hợp lệ hoặc thiếu thông tin.",
                        java.util.List.of()
                );
            }
            String barcode = scannedItem.barcode().trim();
            int quantity = scannedItem.quantity() == null ? 0 : scannedItem.quantity();
            if (quantity <= 0) {
                throw new WarehouseExportValidationException(
                        "BARCODE_NOT_FOUND",
                        "Số lượng quét phải là số nguyên dương.",
                        java.util.List.of()
                );
            }
            scannedTotals.merge(barcode, quantity, (existing, next) -> (existing == null ? 0 : existing) + next);
        }

        Map<String, ProductMatch> approvedBarcodeMap = new java.util.HashMap<>();
        for (Map<String, Object> item : approvedItems) {
            UUID productId = UUID.fromString(String.valueOf(item.get("product_id")));
            int approvedQuantity = ((Number) item.get("approved_quantity")).intValue();
            String barcode = findProductBarcodeById(productId);
            if (barcode != null && !barcode.isBlank()) {
                approvedBarcodeMap.put(barcode.trim(), new ProductMatch(productId, approvedQuantity, findProductNameById(productId), findProductUnitById(productId)));
            }
        }

        List<ScanErrorDetail> errors = new java.util.ArrayList<>();
        for (Map.Entry<String, Integer> entry : scannedTotals.entrySet()) {
            String barcode = entry.getKey();
            Integer scannedQuantity = entry.getValue();
            ProductMatch match = approvedBarcodeMap.get(barcode);
            if (match == null) {
                String productName = findProductNameByBarcode(barcode);
                if (productName == null) {
                    errors.add(new ScanErrorDetail(null, null, null, scannedQuantity, null, null));
                    continue;
                }
                errors.add(new ScanErrorDetail(productName, findProductUnitByBarcode(barcode), null, scannedQuantity, null, null));
                continue;
            }

            if (match.approvedQuantity() <= 0) {
                errors.add(new ScanErrorDetail(match.productName(), match.unit(), 0, scannedQuantity, 0, null));
                continue;
            }

            if (scannedQuantity < match.approvedQuantity()) {
                errors.add(new ScanErrorDetail(match.productName(), match.unit(), match.approvedQuantity(), scannedQuantity, match.approvedQuantity() - scannedQuantity, null));
            } else if (scannedQuantity > match.approvedQuantity()) {
                errors.add(new ScanErrorDetail(match.productName(), match.unit(), match.approvedQuantity(), scannedQuantity, null, scannedQuantity - match.approvedQuantity()));
            }
        }

        if (!errors.isEmpty()) {
            throw new WarehouseExportValidationException(
                    errors.stream().anyMatch(detail -> detail.missingQuantity() != null) ? "SCAN_INCOMPLETE" :
                            (errors.stream().anyMatch(detail -> detail.excessQuantity() != null) ? "SCAN_EXCEEDED" : "BARCODE_NOT_FOUND"),
                    errors.stream().anyMatch(detail -> detail.missingQuantity() != null)
                            ? "Chưa kiểm tra đủ số lượng sản phẩm."
                            : (errors.stream().anyMatch(detail -> detail.excessQuantity() != null) ? "Số lượng quét vượt quá số lượng duyệt." : "Dữ liệu barcode không hợp lệ."),
                    errors
            );
        }

        for (Map<String, Object> item : approvedItems) {
            UUID productId = UUID.fromString(String.valueOf(item.get("product_id")));
            int approvedQuantity = ((Number) item.get("approved_quantity")).intValue();
            String barcode = findProductBarcodeById(productId);
            if (barcode == null || barcode.isBlank()) {
                throw new WarehouseExportValidationException(
                        "BARCODE_NOT_CONFIGURED",
                        "Sản phẩm chưa có barcode được cấu hình.",
                        java.util.List.of(new ScanErrorDetail(findProductNameById(productId), findProductUnitById(productId), approvedQuantity, 0, approvedQuantity, null))
                );
            }
            Integer scannedQuantity = scannedTotals.get(barcode.trim());
            if (scannedQuantity == null || !scannedQuantity.equals(approvedQuantity)) {
                throw new WarehouseExportValidationException(
                        scannedQuantity == null ? "BARCODE_NOT_IN_REQUEST" : "SCAN_INCOMPLETE",
                        scannedQuantity == null ? "Barcode không thuộc phiếu yêu cầu." : "Chưa kiểm tra đủ số lượng sản phẩm.",
                        java.util.List.of(new ScanErrorDetail(findProductNameById(productId), findProductUnitById(productId), approvedQuantity, scannedQuantity == null ? 0 : scannedQuantity, scannedQuantity == null ? approvedQuantity : Math.max(approvedQuantity - scannedQuantity, 0), scannedQuantity != null && scannedQuantity > approvedQuantity ? scannedQuantity - approvedQuantity : null))
                );
            }
        }
    }

    private String findProductBarcodeById(UUID productId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT barcode FROM public.products WHERE id = ?",
                    String.class,
                    productId
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private String findProductNameById(UUID productId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT name FROM public.products WHERE id = ?",
                    String.class,
                    productId
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private String findProductUnitById(UUID productId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT package_size FROM public.products WHERE id = ?",
                    String.class,
                    productId
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private String findProductNameByBarcode(String barcode) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT name FROM public.products WHERE barcode = ?",
                    String.class,
                    barcode
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private String findProductUnitByBarcode(String barcode) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT package_size FROM public.products WHERE barcode = ?",
                    String.class,
                    barcode
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private InsufficientStockException insufficientStock(UUID productId, int requiredQuantity, int availableQuantity) {
        String productName = jdbcTemplate.queryForObject(
                "SELECT name FROM public.products WHERE id = ?",
                String.class,
                productId
        );
        return new InsufficientStockException(productName, requiredQuantity, availableQuantity);
    }

    private InsufficientStockException extractInsufficientStockError(String dbMessage, List<Map<String, Object>> approvedItems, UUID warehouseId) {
        for (Map<String, Object> item : approvedItems) {
            UUID productId = UUID.fromString(String.valueOf(item.get("product_id")));
            int requiredQuantity = ((Number) item.get("approved_quantity")).intValue();
            Integer availableQuantity = jdbcTemplate.queryForObject(
                    "SELECT quantity FROM public.warehouse_stock WHERE warehouse_id = ? AND product_id = ?",
                    Integer.class,
                    warehouseId,
                    productId
            );
            if (availableQuantity == null || availableQuantity < requiredQuantity) {
                return insufficientStock(productId, requiredQuantity, availableQuantity == null ? 0 : availableQuantity);
            }
        }
        return new InsufficientStockException("Sản phẩm", 0, 0);
    }

    public static final class InsufficientStockException extends RuntimeException {
        private final String productName;
        private final int requiredQuantity;
        private final int availableQuantity;

        public InsufficientStockException(String productName, int requiredQuantity, int availableQuantity) {
            super("Sản phẩm " + productName + " cần xuất " + requiredQuantity + " nhưng kho chỉ còn " + availableQuantity + ".");
            this.productName = productName;
            this.requiredQuantity = requiredQuantity;
            this.availableQuantity = availableQuantity;
        }

        public String getProductName() {
            return productName;
        }

        public int getRequiredQuantity() {
            return requiredQuantity;
        }

        public int getAvailableQuantity() {
            return availableQuantity;
        }
    }

    public static final class WarehouseExportValidationException extends RuntimeException {
        private final String code;
        private final List<ScanErrorDetail> details;

        public WarehouseExportValidationException(String code, String message, List<ScanErrorDetail> details) {
            super(message);
            this.code = code;
            this.details = details == null ? java.util.List.of() : details;
        }

        public String getCode() {
            return code;
        }

        public List<ScanErrorDetail> getDetails() {
            return details;
        }
    }

    public record ExportRequest(List<ScanItem> scannedItems) {
    }

    public record ScanItem(String barcode, Integer quantity) {
    }

    public record ScanErrorDetail(
            String productName,
            String unit,
            Integer approvedQuantity,
            Integer scannedQuantity,
            Integer missingQuantity,
            Integer excessQuantity
    ) {
    }

    public record ProductMatch(UUID productId, int approvedQuantity, String productName, String unit) {
    }

    public record ApproveRequest(List<ApproveItem> items) {
    }

    public record RejectRequest(String reason) {
    }

    public record ApproveItem(UUID productId, Integer approvedQuantity) {
    }

    public record RestockRequestSummaryResponse(
            UUID id,
            UUID storeId,
            String storeName,
            UUID warehouseId,
            String warehouseName,
            String status,
            String note,
            OffsetDateTime createdAt,
            UUID createdBy,
            String createdByName,
            int itemCount,
            int totalRequestedQuantity,
            OffsetDateTime approvedAt,
            String rejectReason
    ) {
    }

    public record RestockRequestItemDetail(
            UUID productId,
            String productName,
            String barcode,
            String imageUrl,
            String unit,
            int requestedQuantity,
            Integer approvedQuantity,
            int warehouseStockQuantity,
            boolean active
    ) {
        public RestockRequestItemDetail(
                UUID productId,
                String productName,
                String barcode,
                String imageUrl,
                int requestedQuantity,
                Integer approvedQuantity,
                int warehouseStockQuantity,
                boolean active
        ) {
            this(productId, productName, barcode, imageUrl, null, requestedQuantity, approvedQuantity, warehouseStockQuantity, active);
        }
    }

    public record RestockRequestDetailResponse(
            UUID id,
            UUID storeId,
            String storeName,
            UUID warehouseId,
            String warehouseName,
            String status,
            String note,
            String rejectReason,
            UUID createdBy,
            String createdByName,
            UUID approvedBy,
            String approvedByName,
            String exportCreatedByName,
            String receivedByName,
            OffsetDateTime createdAt,
            OffsetDateTime approvedAt,
            List<RestockRequestItemDetail> items,
            UUID exportId,
            String exportStatus,
            OffsetDateTime exportCreatedAt,
            OffsetDateTime receivedAt
    ) {
        public RestockRequestDetailResponse(
                UUID id,
                UUID storeId,
                String storeName,
                UUID warehouseId,
                String warehouseName,
                String status,
                String note,
                String rejectReason,
                UUID createdBy,
                String createdByName,
                UUID approvedBy,
                OffsetDateTime createdAt,
                OffsetDateTime approvedAt,
                List<RestockRequestItemDetail> items,
                UUID exportId,
                String exportStatus
        ) {
            this(id, storeId, storeName, warehouseId, warehouseName, status, note, rejectReason,
                    createdBy, createdByName, approvedBy, null, null, null,
                    createdAt, approvedAt, items, exportId, exportStatus, null, null);
        }
    }

    private OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().atOffset(ZoneOffset.UTC);
        }
        if (value instanceof Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }
        if (value instanceof java.time.LocalDateTime localDateTime) {
            return localDateTime.atOffset(ZoneOffset.UTC);
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        }
        if (value instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            return OffsetDateTime.parse(trimmed);
        }
        return OffsetDateTime.parse(String.valueOf(value));
    }

    public record ApproveResult(UUID requestId, String status) {
    }

    public record RejectResult(UUID requestId, String status) {
    }

    public record ExportResult(
            UUID exportId,
            UUID requestId,
            String status,
            String requestStatus,
            int totalProducts,
            int totalQuantity,
            List<ExportItemSummary> items
    ) {
        public ExportResult(UUID exportId, String status) {
            this(exportId, null, status, null, 0, 0, java.util.List.<ExportItemSummary>of());
        }
    }

    public record ExportItemSummary(
            String productName,
            String barcode,
            String unit,
            int approvedQuantity,
            int scannedQuantity,
            int exportedQuantity
    ) {
    }
}
