package vn.team4.store.repository;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

import vn.team4.store.model.Product;

@Repository("firestoreProductRepository")
public class ProductRepository {
    private final Firestore firestore;

    public ProductRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<Product> findAll() throws Exception {
        QuerySnapshot snapshot = firestore
                .collection("products")
                .get()
                .get();

        List<Product> products = new ArrayList<>();

        for (QueryDocumentSnapshot document
                : snapshot.getDocuments()) {

            Product product = toProduct(document);
            product.setId(document.getId());

            products.add(product);
        }

        return products;
    }

    private Product toProduct(QueryDocumentSnapshot document) {
        Product product = new Product();
        product.setName(stringValue(document, "name"));
        product.setPrice(numberValue(document, "price"));
        product.setOldPrice(nullableNumberValue(document, "oldPrice"));
        product.setImportPrice(numberValue(document, "importPrice"));
        product.setBarcode(barcodeValue(document));
        product.setRating(numberValue(document, "rating"));
        product.setCategory(stringValue(document, "category"));
        product.setImageUrl(stringValue(document, "imageUrl"));
        product.setBrand(stringValue(document, "brand"));
        product.setPackageSize(stringValue(document, "packageSize"));
        product.setDescription(stringValue(document, "description"));
        product.setStock((int) numberValue(document, "stock"));
        return product;
    }

    private String stringValue(QueryDocumentSnapshot document, String field) {
        Object value = document.get(field);
        if (value == null || value instanceof String) {
            return (String) value;
        }
        throw new IllegalStateException(
                "Firestore field '%s' must be a string".formatted(field)
        );
    }

    private double numberValue(QueryDocumentSnapshot document, String field) {
        Object value = document.get(field);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalStateException(
                "Firestore field '%s' must be numeric".formatted(field)
        );
    }

    private Double nullableNumberValue(QueryDocumentSnapshot document, String field) {
        Object value = document.get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalStateException(
                "Firestore field '%s' must be numeric".formatted(field)
        );
    }

    private String barcodeValue(QueryDocumentSnapshot document) {
        Object barcode = document.get("barcode");
        if (barcode == null || barcode instanceof String) {
            return (String) barcode;
        }
        if (barcode instanceof Number) {
            return barcode.toString();
        }
        throw new IllegalStateException(
                "Firestore field 'barcode' must be a string or number"
        );
    }

    public Product save(Product product) throws Exception {
        DocumentReference document = firestore
                .collection("products")
                .document();

        product.setId(document.getId());

        document.set(product).get();

        return product;
    } 
}
