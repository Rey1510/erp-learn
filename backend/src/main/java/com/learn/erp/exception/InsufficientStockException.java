package com.learn.erp.exception;

public class InsufficientStockException extends RuntimeException {
    private final Long productId;
    private final String productName;
    private final Integer availableStock;
    private final Integer requestedQuantity;

    public InsufficientStockException(Long productId, String productName, Integer availableStock, Integer requestedQuantity) {
        super(String.format("Stok produk '%s' tidak mencukupi (sisa: %d, diminta: %d)", productName, availableStock, requestedQuantity));
        this.productId = productId;
        this.productName = productName;
        this.availableStock = availableStock;
        this.requestedQuantity = requestedQuantity;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }
}
