package com.example.storebe.product.repository;

import com.example.storebe.product.entity.Product;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class ProductRepository {
    private final ConcurrentMap<UUID, Product> products = new ConcurrentHashMap<>();

    public List<Product> findAll() {
        return List.copyOf(products.values());
    }

    public Optional<Product> findById(UUID id) {
        return Optional.ofNullable(products.get(id));
    }

    public Product save(Product product) {
        products.put(product.id(), product);
        return product;
    }

    public void deleteById(UUID id) {
        products.remove(id);
    }
}