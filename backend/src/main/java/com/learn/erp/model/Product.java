package com.learn.erp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private String status; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product() {}

    public Product(String name, String sku, String category, Double price, Integer stock) {
        this.name = name;
        this.sku = sku;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.status = calculateStatus(stock);
    }

    public Product(Long id, String name, String sku, String category, Double price, Integer stock, String status) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.status = status != null ? status : calculateStatus(stock);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = calculateStatus(this.stock);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.status = calculateStatus(this.stock);
    }

    public static String calculateStatus(Integer stock) {
        if (stock == null || stock <= 0) return "OUT_OF_STOCK";
        if (stock <= 5) return "LOW_STOCK";
        return "IN_STOCK";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { 
        this.stock = stock; 
        this.status = calculateStatus(stock);
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
