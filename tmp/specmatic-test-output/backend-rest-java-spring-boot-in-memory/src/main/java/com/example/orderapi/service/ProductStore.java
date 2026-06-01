package com.example.orderapi.service;

import com.example.orderapi.model.Product;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ProductStore {
    private final Map<Integer, Product> products = new LinkedHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(100);

    public ProductStore() {
        products.put(10, new Product(10, "XYZ Phone", "gadget", 10, "2023-10-01"));
        products.put(20, new Product(20, "Deleted Product", "other", 1, "2023-10-01"));
    }

    public List<Product> findAll(String type) {
        return products.values().stream()
                .filter(p -> type == null || p.getType().equals(type))
                .collect(Collectors.toList());
    }

    public Optional<Product> findById(int id) {
        return Optional.ofNullable(products.get(id));
    }

    public Product create(Product product) {
        int id = idCounter.getAndIncrement();
        product.setId(id);
        product.setCreatedOn(LocalDate.now().toString());
        products.put(id, product);
        return product;
    }

    public boolean update(int id, Product updates) {
        Product existing = products.get(id);
        if (existing == null) return false;
        existing.setName(updates.getName());
        existing.setType(updates.getType());
        existing.setInventory(updates.getInventory());
        return true;
    }

    public boolean delete(int id) {
        return products.remove(id) != null;
    }

    public boolean exists(int id) {
        return products.containsKey(id);
    }
}
