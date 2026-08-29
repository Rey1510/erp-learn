package com.learn.erp.controller;

import com.learn.erp.dto.CreateOrderRequest;
import com.learn.erp.exception.InsufficientStockException;
import com.learn.erp.model.Order;
import com.learn.erp.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001"})
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/paged")
    public ResponseEntity<com.learn.erp.dto.PageResponse<Order>> getPaginatedOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.getPaginatedOrders(page, size, sortBy, direction, search, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyHeader) {
        try {
            if (idempotencyHeader != null && !idempotencyHeader.isBlank()) {
                request.setIdempotencyKey(idempotencyHeader.trim());
            }
            Order created = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (InsufficientStockException e) {
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("code", "INSUFFICIENT_STOCK");
            errorDetails.put("error", e.getMessage());
            errorDetails.put("productId", e.getProductId());
            errorDetails.put("productName", e.getProductName());
            errorDetails.put("availableStock", e.getAvailableStock());
            errorDetails.put("requestedQuantity", e.getRequestedQuantity());
            return ResponseEntity.badRequest().body(errorDetails);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String status = body.get("status");
            if (status == null || status.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Status harus diisi."));
            }
            Order updated = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/reseed")
    public ResponseEntity<List<Order>> reseedSampleOrders() {
        return ResponseEntity.ok(orderService.reseedSampleOrders());
    }
}
