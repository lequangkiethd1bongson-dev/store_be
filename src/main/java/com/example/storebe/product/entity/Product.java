package com.example.storebe.product.entity;

import java.math.BigDecimal;
import java.util.UUID;

public record Product(UUID id, String name, String description, BigDecimal price) {
}