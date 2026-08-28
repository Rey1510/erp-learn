package com.learn.erp.dto;

import java.util.List;

public class CreateOrderRequest {
    private String customerName;
    private String customerEmail;
    private List<OrderItemDto> items;

    public CreateOrderRequest() {}

    public CreateOrderRequest(String customerName, String customerEmail, List<OrderItemDto> items) {
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.items = items;
    }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }
}
