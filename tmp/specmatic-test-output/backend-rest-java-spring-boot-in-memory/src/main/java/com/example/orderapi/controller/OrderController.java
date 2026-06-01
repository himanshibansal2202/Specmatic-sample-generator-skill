package com.example.orderapi.controller;

import com.example.orderapi.model.Order;
import com.example.orderapi.service.OrderStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class OrderController {

    private static final Set<String> VALID_STATUSES = Set.of("fulfilled", "pending", "cancelled");
    private final OrderStore orderStore;

    public OrderController(OrderStore orderStore) {
        this.orderStore = orderStore;
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getOrders() {
        return ResponseEntity.ok(orderStore.findAll());
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable int id) {
        return orderStore.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of()));
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authenticate", required = false) String auth,
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
        validateUuid(idempotencyKey, "Idempotency-Key");
        validateOrderBody(body);
        Order order = new Order();
        order.setProductid(((Number) body.get("productid")).intValue());
        order.setCount(((Number) body.get("count")).intValue());
        Order created = orderStore.create(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", created.getId()));
    }

    @PatchMapping("/orders/{id}")
    public ResponseEntity<?> updateOrder(
            @PathVariable int id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authenticate", required = false) String auth) {
        validateOrderUpdateBody(body);
        if (!orderStore.exists(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of());
        }
        Order updates = new Order();
        updates.setProductid(((Number) body.get("productid")).intValue());
        updates.setCount(((Number) body.get("count")).intValue());
        updates.setStatus((String) body.get("status"));
        orderStore.update(id, updates);
        return ResponseEntity.ok("success");
    }

    @DeleteMapping("/orders/{id}")
    public ResponseEntity<?> deleteOrder(
            @PathVariable int id,
            @RequestHeader(value = "Authenticate", required = false) String auth) {
        if (!orderStore.delete(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of());
        }
        return ResponseEntity.ok("success");
    }

    private void validateOrderBody(Map<String, Object> body) {
        if (body.get("productid") == null || !(body.get("productid") instanceof Number)) {
            throw new IllegalArgumentException("productid is required and must be a number");
        }
        if (body.get("count") == null || !(body.get("count") instanceof Number)) {
            throw new IllegalArgumentException("count is required and must be a number");
        }
    }

    private void validateOrderUpdateBody(Map<String, Object> body) {
        validateOrderBody(body);
        if (body.get("status") == null || !(body.get("status") instanceof String)) {
            throw new IllegalArgumentException("status is required and must be a string");
        }
        if (!VALID_STATUSES.contains(body.get("status"))) {
            throw new IllegalArgumentException("Invalid status: " + body.get("status"));
        }
    }

    private void validateUuid(String value, String paramName) {
        try {
            java.util.UUID.fromString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid UUID format for " + paramName + ": " + value);
        }
    }
}
