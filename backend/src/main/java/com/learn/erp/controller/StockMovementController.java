package com.learn.erp.controller;

import com.learn.erp.dto.RestockRequest;
import com.learn.erp.model.StockMovement;
import com.learn.erp.service.StockMovementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock-movements")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001"})
public class StockMovementController {

    private final StockMovementService stockMovementService;

    @Autowired
    public StockMovementController(StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    @GetMapping
    public ResponseEntity<List<StockMovement>> getAllMovements() {
        return ResponseEntity.ok(stockMovementService.getAllMovements());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<StockMovement>> getMovementsByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(stockMovementService.getMovementsByProductId(productId));
    }

    @PostMapping("/restock")
    public ResponseEntity<?> restockProduct(@RequestBody RestockRequest request) {
        try {
            if (request.getProductId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Product ID wajib diisi."));
            }
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Jumlah restock harus lebih dari 0."));
            }

            StockMovement movement = stockMovementService.recordRestock(
                    request.getProductId(),
                    request.getQuantity(),
                    request.getNotes() != null ? request.getNotes() : "Restock Gudang"
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(movement);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
