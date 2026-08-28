package com.learn.erp.service;

import com.learn.erp.model.Product;
import com.learn.erp.model.StockMovement;
import com.learn.erp.repository.ProductRepository;
import com.learn.erp.repository.StockMovementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;

    @Autowired
    public StockMovementService(StockMovementRepository stockMovementRepository, ProductRepository productRepository) {
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
    }

    public List<StockMovement> getMovementsByProductId(Long productId) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public List<StockMovement> getAllMovements() {
        return stockMovementRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public StockMovement logMovement(Product product, String type, int quantityChange, String referenceNumber, String notes) {
        StockMovement movement = new StockMovement(
                product.getId(),
                product.getName(),
                product.getSku(),
                type,
                quantityChange,
                product.getStock(),
                referenceNumber,
                notes
        );
        return stockMovementRepository.save(movement);
    }

    @Transactional
    public StockMovement recordRestock(Long productId, int addedQuantity, String notes) {
        if (addedQuantity <= 0) {
            throw new IllegalArgumentException("Jumlah restock harus lebih besar dari 0.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan dengan ID: " + productId));

        int updatedStock = product.getStock() + addedQuantity;
        product.setStock(updatedStock);
        product.setStatus(Product.calculateStatus(updatedStock));
        productRepository.save(product);

        return logMovement(product, "RESTOCK", addedQuantity, "MANUAL-RESTOCK", notes);
    }
}
