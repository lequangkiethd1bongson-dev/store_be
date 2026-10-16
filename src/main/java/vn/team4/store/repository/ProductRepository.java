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

            Product product = document.toObject(Product.class);
            product.setId(document.getId());

            products.add(product);
        }

        return products;
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
