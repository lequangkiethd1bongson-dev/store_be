package vn.team4.store.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class WarehouseStockId implements Serializable {
    private UUID warehouseId;
    private UUID productId;

    public WarehouseStockId() { }

    public WarehouseStockId(UUID warehouseId, UUID productId) {
        this.warehouseId = warehouseId;
        this.productId = productId;
    }

    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof WarehouseStockId that)) return false;
        return Objects.equals(warehouseId, that.warehouseId)
                && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(warehouseId, productId);
    }
}
