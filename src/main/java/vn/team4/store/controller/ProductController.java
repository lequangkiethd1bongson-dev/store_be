package vn.team4.store.controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.team4.store.dto.ProductRequest;
import vn.team4.store.model.Product;
import vn.team4.store.service.ProductService;

import java.util.List;

@RestController("firestoreProductController")
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;

     public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<Product> getAll() throws Exception {
        return service.getAllProducts();
    }

    @PostMapping
    public ResponseEntity<Product> create(
            @RequestBody ProductRequest request
    ) throws Exception {

        Product product = service.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(product);
    }
}
