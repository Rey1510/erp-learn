package com.learn.erp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String type; // INITIAL, SALE, RESTOCK, CANCEL_RESTOCK, MANUAL_ADJUSTMENT

    @Column(nullable = false)
    private Integer quantityChange; // e.g. +10 or -2

    @Column(nullable = false)
    private Integer resultingStock;

    private String referenceNumber; // e.g. "ORD-20260826-0001" or "PO-WH-992"

    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public StockMovement() {}

    public StockMovement(Long productId, String productName, String sku, String type, 
                         Integer quantityChange, Integer resultingStock, 
                         String referenceNumber, String notes) {
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.type = type;
        this.quantityChange = quantityChange;
        this.resultingStock = resultingStock;
        this.referenceNumber = referenceNumber;
        this.notes = notes;
        this.createdAt = LocalDateTime.now();
    }

    public StockMovement(Long productId, String productName, String sku, String type, 
                         Integer quantityChange, Integer resultingStock, 
                         String referenceNumber, String notes, LocalDateTime createdAt) {
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.type = type;
        this.quantityChange = quantityChange;
        this.resultingStock = resultingStock;
        this.referenceNumber = referenceNumber;
        this.notes = notes;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }

    public Integer getResultingStock() { return resultingStock; }
    public void setResultingStock(Integer resultingStock) { this.resultingStock = resultingStock; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
