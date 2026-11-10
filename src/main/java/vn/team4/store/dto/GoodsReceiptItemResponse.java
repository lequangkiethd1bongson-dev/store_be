package vn.team4.store.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoodsReceiptItemResponse(
        UUID productId,
        String productName,
        String barcode,
        Integer quantity,
        BigDecimal importPrice,
        String lotNo,
        LocalDate expiryDate) {
}
