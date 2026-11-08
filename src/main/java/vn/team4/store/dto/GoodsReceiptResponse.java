package vn.team4.store.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record GoodsReceiptResponse(
        UUID id,
        UUID warehouseId,
        UUID supplierId,
        String supplierName,
        UUID createdBy,
        String createdByName,
        String status,
        BigDecimal totalAmount,
        String note,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt,
        OffsetDateTime cancelledAt,
        List<GoodsReceiptItemResponse> items) {
}
