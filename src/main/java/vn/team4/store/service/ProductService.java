package vn.team4.store.service;

import org.springframework.stereotype.Service;
import vn.team4.store.dto.ProductRequest;
import vn.team4.store.model.Product;
import vn.team4.store.repository.ProductRepository;

import java.util.List;

@Service("firestoreProductService")
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> getAllProducts() throws Exception {
        return repository.findAll();
    }

    public Product create(ProductRequest request) throws Exception {
        if (request.getName() == null
                || request.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Tên sản phẩm không được để trống"
            );
        }

        if (request.getImport_price() < 0
                || request.getQuantity() < 0) {
            throw new IllegalArgumentException(
                    "Giá và số lượng phải >= 0"
            );
        }

        if (request.getSelling_price() < request.getImport_price()) {
            throw new IllegalArgumentException(
                    "Giá bán phải >= giá nhập"
            );
        }

        Product product = new Product(
                null,
                request.getName().trim(),
                request.getImport_price(),
                request.getSelling_price(),
                request.getQuantity(),
                request.getCategory()
        );

        return repository.save(product);
    }
}
