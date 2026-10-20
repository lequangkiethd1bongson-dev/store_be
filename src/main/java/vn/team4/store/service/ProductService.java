package vn.team4.store.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
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

        if (request.getImportPrice() < 0
                || request.getPrice() < 0
                || request.getStock() < 0) {
            throw new IllegalArgumentException(
                    "Giá và số lượng phải >= 0"
            );
        }

        if (request.getPrice() < request.getImportPrice()) {
            throw new IllegalArgumentException(
                    "Giá bán phải >= giá nhập"
            );
        }

        Product product = new Product(
                request.getId(),
                request.getName().trim(),
                request.getPrice(),
                request.getOldPrice(),
                request.getImportPrice(),
                request.getBarcode(),
                request.getRating(),
                request.getCategory(),
                request.getImageUrl(),
                request.getBrand(),
                request.getPackageSize(),
                request.getDescription(),
                request.getStock()
        );

        try {
            return repository.save(product, request.getId());
        } catch (Exception exception) {
            if (isAlreadyExists(exception)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Product already exists: " + request.getId(),
                        exception
                );
            }
            throw exception;
        }
    }

    private boolean isAlreadyExists(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof com.google.api.gax.rpc.AlreadyExistsException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
