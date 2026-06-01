package com.example.orderapi.controller;

import com.example.orderapi.model.Product;
import com.example.orderapi.service.ProductStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class ProductController {

    private static final Set<String> VALID_TYPES = Set.of("book", "food", "gadget", "other");
    private final ProductStore productStore;

    public ProductController(ProductStore productStore) {
        this.productStore = productStore;
    }

    @GetMapping("/products")
    public ResponseEntity<?> getProducts(
            @RequestParam(required = false) String type,
            @RequestHeader(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "from-date", required = false) String fromDate,
            @RequestParam(value = "to-date", required = false) String toDate) {
        if (type != null && !VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException("Invalid type: " + type);
        }
        if (fromDate != null) {
            validateDate(fromDate, "from-date");
        }
        if (toDate != null) {
            validateDate(toDate, "to-date");
        }
        List<Product> products = productStore.findAll(type);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(@PathVariable int id) {
        return productStore.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of()));
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authenticate", required = false) String auth,
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
        validateUuid(idempotencyKey, "Idempotency-Key");
        validateProductBody(body);
        Product product = new Product();
        product.setName((String) body.get("name"));
        product.setType((String) body.get("type"));
        product.setInventory(((Number) body.get("inventory")).intValue());
        Product created = productStore.create(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", created.getId()));
    }

    @PatchMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable int id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authenticate", required = false) String auth) {
        validateProductBody(body);
        if (!productStore.exists(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of());
        }
        Product updates = new Product();
        updates.setName((String) body.get("name"));
        updates.setType((String) body.get("type"));
        updates.setInventory(((Number) body.get("inventory")).intValue());
        productStore.update(id, updates);
        return ResponseEntity.ok("success");
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(
            @PathVariable int id,
            @RequestHeader(value = "Authenticate", required = false) String auth) {
        if (!productStore.delete(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of());
        }
        return ResponseEntity.ok("success");
    }

    @PutMapping(value = "/products/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProductImage(
            @PathVariable int id,
            @RequestParam("image") MultipartFile image) {
        if (!productStore.exists(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of());
        }
        return ResponseEntity.ok(Map.of("message", "Success"));
    }

    private void validateProductBody(Map<String, Object> body) {
        if (body.get("name") == null || !(body.get("name") instanceof String)) {
            throw new IllegalArgumentException("name is required and must be a string");
        }
        if (body.get("type") == null || !(body.get("type") instanceof String)) {
            throw new IllegalArgumentException("type is required and must be a string");
        }
        if (!VALID_TYPES.contains(body.get("type"))) {
            throw new IllegalArgumentException("Invalid type: " + body.get("type"));
        }
        if (body.get("inventory") == null || !(body.get("inventory") instanceof Number)) {
            throw new IllegalArgumentException("inventory is required and must be a number");
        }
        int inventory = ((Number) body.get("inventory")).intValue();
        if (inventory < 1 || inventory > 101) {
            throw new IllegalArgumentException("inventory must be between 1 and 101");
        }
    }

    private void validateDate(String value, String paramName) {
        try {
            java.time.LocalDate.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format for " + paramName + ": " + value);
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
