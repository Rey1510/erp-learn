package com.learn.erp.dto;

public class RestockRequest {
    private Long productId;
    private Integer quantity;
    private String notes;

    public RestockRequest() {}

    public RestockRequest(Long productId, Integer quantity, String notes) {
        this.productId = productId;
        this.quantity = quantity;
        this.notes = notes;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
