package vn.team4.store.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record GoodsReceiptRequest(
        @JsonAlias({"supplier_id", "supplierId"})
        @NotNull(message = "Nhà cung cấp là bắt buộc") UUID supplierId,
        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự") String note,
        @JsonAlias({"items"})
        @NotEmpty(message = "Phiếu nhập phải có ít nhất một sản phẩm") List<@Valid GoodsReceiptItemRequest> items,
        @JsonAlias({"warehouse_id"}) UUID warehouseId) {

    public GoodsReceiptRequest(UUID supplierId, String note, List<GoodsReceiptItemRequest> items) {
        this(supplierId, note, items, null);
    }
}
