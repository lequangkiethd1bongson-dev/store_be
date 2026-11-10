package vn.team4.store.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoodsReceiptItemRequest(
        @JsonAlias({"product_id", "productId"})
        @NotNull(message = "Sản phẩm nhập kho không được để trống") UUID productId,
        @JsonAlias({"quantity"})
        @NotNull(message = "Số lượng nhập kho không được để trống") @Positive(message = "Số lượng phải lớn hơn 0") Integer quantity,
        @JsonAlias({"import_price", "importPrice"})
        @NotNull(message = "Giá nhập không được để trống")
        @DecimalMin(value = "0.01", inclusive = false, message = "Giá nhập phải lớn hơn 0") BigDecimal importPrice,
        @JsonAlias({"lot_no", "lotNo"}) String lotNo,
        @JsonAlias({"expiry_date", "expiryDate"})
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate expiryDate) {
}
