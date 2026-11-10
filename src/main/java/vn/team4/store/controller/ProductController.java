package vn.team4.store.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.team4.store.dto.ProductRequest;
import vn.team4.store.model.Product;
import vn.team4.store.service.ProductService;

import java.util.List;
import java.util.UUID;

@RestController("storeProductController")
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    // Existing Flutter route remains valid. warehouseId is optional.
    @GetMapping
    public List<Product> getAll(
            @RequestParam(required = false) UUID warehouseId) {
        return service.getAllProducts(warehouseId);
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request) {
        Product product = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
}
