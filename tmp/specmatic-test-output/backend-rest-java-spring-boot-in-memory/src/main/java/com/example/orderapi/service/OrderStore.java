package com.example.orderapi.service;

import com.example.orderapi.model.Order;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderStore {
    private final Map<Integer, Order> orders = new LinkedHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(100);

    public OrderStore() {
        orders.put(10, new Order(10, 10, 2, "pending"));
        orders.put(20, new Order(20, 10, 1, "pending"));
    }

    public List<Order> findAll() {
        return new ArrayList<>(orders.values());
    }

    public Optional<Order> findById(int id) {
        return Optional.ofNullable(orders.get(id));
    }

    public Order create(Order order) {
        int id = idCounter.getAndIncrement();
        order.setId(id);
        order.setStatus("pending");
        orders.put(id, order);
        return order;
    }

    public boolean update(int id, Order updates) {
        Order existing = orders.get(id);
        if (existing == null) return false;
        existing.setProductid(updates.getProductid());
        existing.setCount(updates.getCount());
        existing.setStatus(updates.getStatus());
        return true;
    }

    public boolean delete(int id) {
        return orders.remove(id) != null;
    }

    public boolean exists(int id) {
        return orders.containsKey(id);
    }
}
