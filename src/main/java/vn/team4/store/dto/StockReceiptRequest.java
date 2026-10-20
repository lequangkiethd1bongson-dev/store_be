package vn.team4.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public class StockReceiptRequest {
    @NotBlank
    @Pattern(regexp = "^[^/]+$")
    private String productId;

    @Positive
    private int quantity;

    public StockReceiptRequest() {
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
