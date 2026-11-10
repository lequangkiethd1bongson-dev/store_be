package vn.team4.store.service;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.team4.store.dto.GoodsReceiptItemRequest;
import vn.team4.store.dto.GoodsReceiptItemResponse;
import vn.team4.store.dto.GoodsReceiptRequest;
import vn.team4.store.dto.GoodsReceiptResponse;
import vn.team4.store.model.EmployeeContext;
import vn.team4.store.model.GoodsReceiptEntity;
import vn.team4.store.model.GoodsReceiptItemEntity;
import vn.team4.store.model.GoodsReceiptStatus;
import vn.team4.store.model.ProductEntity;
import vn.team4.store.supplier.entity.SupplierEntity;
import vn.team4.store.repository.GoodsReceiptItemRepository;
import vn.team4.store.repository.GoodsReceiptRepository;
import vn.team4.store.repository.ProductRepository;
import vn.team4.store.supplier.repository.SupplierRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GoodsReceiptService {

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final GoodsReceiptItemRepository goodsReceiptItemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final EmployeeContextService employeeContextService;
    private final JdbcTemplate jdbcTemplate;

    public GoodsReceiptService(
            GoodsReceiptRepository goodsReceiptRepository,
            GoodsReceiptItemRepository goodsReceiptItemRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            EmployeeContextService employeeContextService,
            JdbcTemplate jdbcTemplate) {
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.goodsReceiptItemRepository = goodsReceiptItemRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.employeeContextService = employeeContextService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<GoodsReceiptResponse> list(Authentication authentication, String status) {
        return list(authentication, status, null);
    }

    @Transactional(readOnly = true)
    public List<GoodsReceiptResponse> list(
            Authentication authentication,
            String status,
            UUID requestedWarehouseId) {
        EmployeeContext employee = employeeContextService.requireWarehouseManagerOrStaff(authentication);
        UUID warehouseScope = employeeContextService.resolveWarehouseScope(employee, requestedWarehouseId);
        if (employeeContextService.isAdmin(employee) && requestedWarehouseId != null) {
            validateWarehouse(requestedWarehouseId);
        }
        GoodsReceiptStatus parsedStatus = status == null || status.isBlank() ? null : parseStatus(status);
        List<GoodsReceiptEntity> receipts;
        if (warehouseScope == null) {
            receipts = parsedStatus == null
                    ? goodsReceiptRepository.findAllByOrderByCreatedAtDesc()
                    : goodsReceiptRepository.findByStatusOrderByCreatedAtDesc(parsedStatus);
        } else {
            receipts = parsedStatus == null
                    ? goodsReceiptRepository.findByWarehouseIdOrderByCreatedAtDesc(warehouseScope)
                    : goodsReceiptRepository.findByWarehouseIdAndStatusOrderByCreatedAtDesc(
                            warehouseScope,
                            parsedStatus);
        }
        return receipts.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GoodsReceiptResponse getById(Authentication authentication, UUID receiptId) {
        EmployeeContext employee = employeeContextService.requireWarehouseManagerOrStaff(authentication);
        GoodsReceiptEntity receipt = findReceipt(employee, receiptId);
        return toResponse(receipt);
    }

    @Transactional
    public GoodsReceiptResponse createDraft(Authentication authentication, GoodsReceiptRequest request) {
        EmployeeContext employee = employeeContextService.requireWarehouseManagerOrStaff(authentication);
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phiếu nhập phải có ít nhất một sản phẩm");
        }
        UUID warehouseId = employeeContextService.resolveWarehouseScope(employee, request.warehouseId());
        if (warehouseId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin phải chọn warehouseId để tạo phiếu nhập");
        }
        if (employeeContextService.isAdmin(employee)) {
            validateWarehouse(warehouseId);
        }
        validateItemRequests(request.items());

        SupplierEntity supplier = validateSupplier(request.supplierId());
        GoodsReceiptEntity receipt = new GoodsReceiptEntity();
        receipt.setWarehouseId(warehouseId);
        receipt.setSupplierId(supplier == null ? null : supplier.getId());
        receipt.setCreatedBy(employee.id());
        receipt.setStatus(GoodsReceiptStatus.draft);
        receipt.setNote(normalizeNote(request.note()));
        receipt.setTotalAmount(BigDecimal.ZERO);
        GoodsReceiptEntity savedReceipt = goodsReceiptRepository.saveAndFlush(receipt);

        List<GoodsReceiptItemEntity> itemEntities = new ArrayList<>();
        for (GoodsReceiptItemRequest itemRequest : request.items()) {
            ProductEntity product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm không tồn tại: " + itemRequest.productId()));
            if (!product.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm đã bị vô hiệu hóa: " + product.getName());
            }
            if (itemRequest.quantity() == null || itemRequest.quantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng sản phẩm phải lớn hơn 0");
            }
            if (itemRequest.importPrice() == null || itemRequest.importPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá nhập phải lớn hơn 0 cho sản phẩm: " + product.getName());
            }

            GoodsReceiptItemEntity itemEntity = new GoodsReceiptItemEntity();
            itemEntity.setReceiptId(savedReceipt.getId());
            itemEntity.setProductId(product.getId());
            itemEntity.setQuantity(itemRequest.quantity());
            itemEntity.setImportPrice(itemRequest.importPrice());
            itemEntity.setLotNo(normalizeNullable(itemRequest.lotNo()));
            itemEntity.setExpiryDate(itemRequest.expiryDate());
            itemEntities.add(itemEntity);
        }

        goodsReceiptItemRepository.saveAllAndFlush(itemEntities);
        savedReceipt.setTotalAmount(calculateTotal(itemEntities));
        goodsReceiptRepository.saveAndFlush(savedReceipt);
        return toResponse(savedReceipt);
    }

    @Transactional
    public GoodsReceiptResponse updateDraft(Authentication authentication, UUID receiptId, GoodsReceiptRequest request) {
        EmployeeContext employee = employeeContextService.requireWarehouseManagerOrStaff(authentication);
        GoodsReceiptEntity receipt = findReceipt(employee, receiptId);
        if (receipt.getStatus() != GoodsReceiptStatus.draft) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ cập nhật được phiếu ở trạng thái nháp");
        }
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phiếu nhập phải có ít nhất một sản phẩm");
        }
        validateItemRequests(request.items());

        SupplierEntity supplier = validateSupplier(request.supplierId());
        receipt.setSupplierId(supplier == null ? null : supplier.getId());
        receipt.setNote(normalizeNote(request.note()));

        List<GoodsReceiptItemEntity> existingItems = goodsReceiptItemRepository.findByReceiptIdOrderByProductId(receiptId);
        if (!existingItems.isEmpty()) {
            goodsReceiptItemRepository.deleteAll(existingItems);
        }

        List<GoodsReceiptItemEntity> refreshedItems = new ArrayList<>();
        for (GoodsReceiptItemRequest itemRequest : request.items()) {
            ProductEntity product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm không tồn tại: " + itemRequest.productId()));
            if (!product.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm đã bị vô hiệu hóa");
            }
            GoodsReceiptItemEntity itemEntity = new GoodsReceiptItemEntity();
            itemEntity.setReceiptId(receiptId);
            itemEntity.setProductId(product.getId());
            itemEntity.setQuantity(itemRequest.quantity());
            itemEntity.setImportPrice(itemRequest.importPrice());
            itemEntity.setLotNo(normalizeNullable(itemRequest.lotNo()));
            itemEntity.setExpiryDate(itemRequest.expiryDate());
            refreshedItems.add(itemEntity);
        }

        goodsReceiptItemRepository.saveAllAndFlush(refreshedItems);
        receipt.setTotalAmount(calculateTotal(refreshedItems));
        goodsReceiptRepository.saveAndFlush(receipt);
        return toResponse(receipt);
    }

    @Transactional
    public GoodsReceiptResponse complete(Authentication authentication, UUID receiptId) {
        EmployeeContext employee = employeeContextService.requireWarehouseManagerOrStaff(authentication);

        Map<String, Object> receiptRow;
        try {
            String sql = "SELECT id, status, warehouse_id FROM public.goods_receipts WHERE id = ?";
            if (!employeeContextService.isAdmin(employee)) {
                sql += " AND warehouse_id = ?";
                receiptRow = jdbcTemplate.queryForMap(sql + " FOR UPDATE", receiptId, employee.warehouseId());
            } else {
                receiptRow = jdbcTemplate.queryForMap(sql + " FOR UPDATE", receiptId);
            }
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu nhập kho");
        }
        UUID warehouseId = UUID.fromString(String.valueOf(receiptRow.get("warehouse_id")));

        Object receiptStatus = receiptRow.get("status");
        if (receiptStatus == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Phiếu nhập cũ chưa được phân loại trạng thái; cần kiểm tra dữ liệu trước khi hoàn tất");
        }
        GoodsReceiptStatus status = parseStatus(String.valueOf(receiptStatus));
        if (status != GoodsReceiptStatus.draft) {
            if (status == GoodsReceiptStatus.completed) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu nhập đã hoàn tất, không thể xác nhận lại");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể hoàn tất phiếu ở trạng thái nháp");
        }

        List<GoodsReceiptItemEntity> items = goodsReceiptItemRepository.findByReceiptIdOrderByProductId(receiptId);
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phiếu nhập không có sản phẩm nào");
        }

        for (GoodsReceiptItemEntity item : items) {
            ProductEntity product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Sản phẩm không tồn tại: " + item.getProductId()));
            if (!product.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Sản phẩm đã bị vô hiệu hóa: " + product.getName());
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Số lượng sản phẩm phải lớn hơn 0");
            }
            if (item.getImportPrice() == null || item.getImportPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Giá nhập phải lớn hơn 0 cho sản phẩm: " + product.getName());
            }

            jdbcTemplate.update(
                    """
                    INSERT INTO public.warehouse_stock (warehouse_id, product_id, quantity, updated_at)
                    VALUES (?, ?, ?, now())
                    ON CONFLICT (warehouse_id, product_id)
                    DO UPDATE SET quantity = public.warehouse_stock.quantity + EXCLUDED.quantity,
                                  updated_at = now()
                    """,
                    warehouseId,
                    product.getId(),
                    item.getQuantity()
            );

            jdbcTemplate.update(
                    "UPDATE public.products SET import_price = ? WHERE id = ?",
                    item.getImportPrice(),
                    product.getId()
            );
        }

        GoodsReceiptEntity receipt = goodsReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu nhập kho"));
        BigDecimal totalAmount = calculateTotal(items);
        receipt.setTotalAmount(totalAmount);
        receipt.setStatus(GoodsReceiptStatus.completed);
        receipt.setCompletedAt(OffsetDateTime.now());
        receipt.setCancelledAt(null);
        goodsReceiptRepository.saveAndFlush(receipt);
        return toResponse(receipt);
    }

    @Transactional
    public GoodsReceiptResponse cancel(Authentication authentication, UUID receiptId) {
        EmployeeContext employee = employeeContextService.requireWarehouseManagerOrStaff(authentication);

        Map<String, Object> receiptRow;
        try {
            String sql = "SELECT status FROM public.goods_receipts WHERE id = ?";
            if (!employeeContextService.isAdmin(employee)) {
                sql += " AND warehouse_id = ?";
                receiptRow = jdbcTemplate.queryForMap(sql + " FOR UPDATE", receiptId, employee.warehouseId());
            } else {
                receiptRow = jdbcTemplate.queryForMap(sql + " FOR UPDATE", receiptId);
            }
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu nhập kho");
        }
        if (receiptRow.get("status") == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Phiếu nhập cũ chưa được phân loại trạng thái; cần kiểm tra dữ liệu trước khi hủy");
        }

        GoodsReceiptEntity receipt = findReceipt(employee, receiptId);
        if (receipt.getStatus() != GoodsReceiptStatus.draft) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể hủy phiếu ở trạng thái nháp");
        }

        receipt.setStatus(GoodsReceiptStatus.cancelled);
        receipt.setCancelledAt(OffsetDateTime.now());
        goodsReceiptRepository.saveAndFlush(receipt);
        return toResponse(receipt);
    }

    private GoodsReceiptEntity findReceipt(EmployeeContext employee, UUID receiptId) {
        return (employeeContextService.isAdmin(employee)
                ? goodsReceiptRepository.findById(receiptId)
                : goodsReceiptRepository.findByIdAndWarehouseId(receiptId, employee.warehouseId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy phiếu nhập kho"));
    }

    private void validateWarehouse(UUID warehouseId) {
        Integer warehouseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.warehouses WHERE id = ?",
                Integer.class,
                warehouseId);
        if (warehouseCount == null || warehouseCount == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy kho");
        }
    }

    private GoodsReceiptResponse toResponse(GoodsReceiptEntity receipt) {
        List<GoodsReceiptItemEntity> items = goodsReceiptItemRepository.findByReceiptIdOrderByProductId(receipt.getId());
        List<GoodsReceiptItemResponse> itemResponses = new ArrayList<>();
        for (GoodsReceiptItemEntity item : items) {
            ProductEntity product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalStateException("Product not found for receipt item: " + item.getProductId()));
            itemResponses.add(new GoodsReceiptItemResponse(
                    product.getId(),
                    product.getName(),
                    product.getBarcode(),
                    item.getQuantity(),
                    item.getImportPrice(),
                    item.getLotNo(),
                    item.getExpiryDate()));
        }

        SupplierEntity supplier = receipt.getSupplierId() == null
                ? null
                : supplierRepository.findById(receipt.getSupplierId()).orElse(null);

        String createdByName = resolveEmployeeName(receipt.getCreatedBy());

        return new GoodsReceiptResponse(
                receipt.getId(),
                receipt.getWarehouseId(),
                receipt.getSupplierId(),
                supplier == null ? null : supplier.getName(),
                receipt.getCreatedBy(),
                createdByName,
                receipt.getStatus() == null ? null : receipt.getStatus().name(),
                receipt.getTotalAmount(),
                receipt.getNote(),
                receipt.getCreatedAt(),
                receipt.getCompletedAt(),
                receipt.getCancelledAt(),
                itemResponses);
    }

    private SupplierEntity validateSupplier(UUID supplierId) {
        if (supplierId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhà cung cấp là bắt buộc");
        }
        SupplierEntity supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhà cung cấp không tồn tại: " + supplierId));
        if (!supplier.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhà cung cấp hiện đang bị vô hiệu hóa");
        }
        return supplier;
    }

    private void validateItemRequests(List<GoodsReceiptItemRequest> items) {
        java.util.Set<UUID> seenProductIds = new java.util.HashSet<>();
        for (GoodsReceiptItemRequest item : items) {
            if (item == null || item.productId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm nhập kho không được để trống");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng sản phẩm phải lớn hơn 0");
            }
            if (item.importPrice() == null || item.importPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá nhập phải lớn hơn 0 cho sản phẩm: " + item.productId());
            }
            if (!seenProductIds.add(item.productId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm bị lặp trong phiếu nhập: " + item.productId());
            }
        }
    }

    private BigDecimal calculateTotal(List<GoodsReceiptItemEntity> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (GoodsReceiptItemEntity item : items) {
            if (item.getImportPrice() == null) {
                continue;
            }
            total = total.add(item.getImportPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total;
    }

    private String normalizeNote(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveEmployeeName(UUID employeeId) {
        if (employeeId == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT full_name FROM public.employees WHERE id = ?",
                    String.class,
                    employeeId
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private GoodsReceiptStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái phiếu nhập không hợp lệ");
        }
        try {
            return GoodsReceiptStatus.valueOf(value.trim().toLowerCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái phiếu nhập không hợp lệ");
        }
    }
}
