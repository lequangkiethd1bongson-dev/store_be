package vn.team4.store.dto;

public record StockReceiptResponse(
        String id,
        String productId,
        int quantity,
        int stockAfter,
        String createdAt
) {
}
