package vn.team4.store.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class GoodsReceiptItemId implements Serializable {

    private UUID receiptId;
    private UUID productId;

    public GoodsReceiptItemId() {
    }

    public GoodsReceiptItemId(UUID receiptId, UUID productId) {
        this.receiptId = receiptId;
        this.productId = productId;
    }

    public UUID getReceiptId() { return receiptId; }
    public void setReceiptId(UUID receiptId) { this.receiptId = receiptId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoodsReceiptItemId that)) return false;
        return Objects.equals(receiptId, that.receiptId) && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receiptId, productId);
    }
}
