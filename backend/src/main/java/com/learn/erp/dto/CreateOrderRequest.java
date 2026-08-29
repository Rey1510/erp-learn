package com.learn.erp.dto;

import java.util.List;

public class CreateOrderRequest {
    private String customerName;
    private String customerEmail;
    private String paymentMethod; // CASH, QRIS, BANK_TRANSFER_VA, CREDIT_CARD
    private String idempotencyKey;
    private List<OrderItemDto> items;

    public CreateOrderRequest() {}

    public CreateOrderRequest(String customerName, String customerEmail, List<OrderItemDto> items) {
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.items = items;
    }

    public CreateOrderRequest(String customerName, String customerEmail, String paymentMethod, List<OrderItemDto> items) {
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.paymentMethod = paymentMethod;
        this.items = items;
    }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }
}
