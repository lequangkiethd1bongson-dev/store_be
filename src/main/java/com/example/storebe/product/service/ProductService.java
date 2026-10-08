package com.example.storebe.product.service;

import com.example.storebe.product.dto.ProductRequest;
import com.example.storebe.product.entity.Product;
import com.example.storebe.product.exception.ProductNotFoundException;
import com.example.storebe.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Product create(ProductRequest request) {
        Product product = new Product(
                UUID.randomUUID(),
                request.name(),
                request.description(),
                request.price()
        );
        return productRepository.save(product);
    }

    public Product update(UUID id, ProductRequest request) {
        findById(id);
        Product product = new Product(id, request.name(), request.description(), request.price());
        return productRepository.save(product);
    }

    public void delete(UUID id) {
        findById(id);
        productRepository.deleteById(id);
    }
}