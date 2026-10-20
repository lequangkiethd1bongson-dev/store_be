package vn.team4.store.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Service;
import vn.team4.store.dto.StockReceiptRequest;
import vn.team4.store.dto.StockReceiptResponse;
import vn.team4.store.model.Product;

import java.time.Instant;
import java.util.Map;

@Service
public class StockReceiptService {
    private final Firestore firestore;

    public StockReceiptService(Firestore firestore) {
        this.firestore = firestore;
    }

    public StockReceiptResponse receive(StockReceiptRequest request) throws Exception {
        String productId = request.getProductId().trim();
        DocumentReference productRef = firestore.collection("products")
                .document(productId);
        DocumentReference receiptRef = firestore.collection("stockReceipts")
                .document();
        String createdAt = Instant.now().toString();

        return firestore.runTransaction(transaction -> {
            DocumentSnapshot productSnapshot = transaction.get(productRef).get();
            int currentStock = 0;

            if (productSnapshot.exists()) {
                Object stockValue = productSnapshot.get("stock");
                if (stockValue != null && !(stockValue instanceof Number)) {
                    throw new IllegalStateException(
                            "Firestore field 'stock' must be numeric for product " + productId
                    );
                }
                if (stockValue instanceof Number number) {
                    long stockLong = number.longValue();
                    if (number.doubleValue() != stockLong
                            || stockLong < 0
                            || stockLong > Integer.MAX_VALUE) {
                        throw new IllegalStateException(
                                "Firestore field 'stock' is outside the supported range for product "
                                        + productId
                        );
                    }
                    currentStock = (int) stockLong;
                }
            }

            int stockAfter = Math.addExact(currentStock, request.getQuantity());
            if (productSnapshot.exists()) {
                transaction.update(productRef, "stock", stockAfter);
            } else {
                Product placeholder = new Product();
                placeholder.setId(productId);
                placeholder.setName("Sản phẩm " + productId);
                placeholder.setCategory("Chưa phân loại");
                placeholder.setStock(stockAfter);
                transaction.create(productRef, placeholder);
            }

            StockReceiptResponse receipt = new StockReceiptResponse(
                    receiptRef.getId(),
                    productId,
                    request.getQuantity(),
                    stockAfter,
                    createdAt
            );
            transaction.set(receiptRef, Map.of(
                    "id", receipt.id(),
                    "productId", receipt.productId(),
                    "quantity", receipt.quantity(),
                    "stockAfter", receipt.stockAfter(),
                    "createdAt", receipt.createdAt()
            ));
            return receipt;
        }).get();
    }
}
