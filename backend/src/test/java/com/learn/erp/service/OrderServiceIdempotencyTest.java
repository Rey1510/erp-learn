package com.learn.erp.service;

import com.learn.erp.dto.CreateOrderRequest;
import com.learn.erp.dto.OrderItemDto;
import com.learn.erp.model.Order;
import com.learn.erp.model.Product;
import com.learn.erp.model.StockMovement;
import com.learn.erp.repository.OrderRepository;
import com.learn.erp.repository.ProductRepository;
import com.learn.erp.repository.StockMovementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderServiceIdempotencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Test
    @DisplayName("Should return identical cached order and avoid double stock deduction when same idempotency key is submitted 5 times")
    void testIdempotentOrderCreation() {
        System.out.println("\n========================================================");
        System.out.println("🛡️ [IDEMPOTENCY TEST] Simulasi 5x Double Submit / Network Retry");
        System.out.println("========================================================");

        // 1. Setup Product with Stock = 10
        String testSku = "IDEM-PROD-" + UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product("Sony WH-1000XM5", testSku, "Audio", 400.0, 10);
        Product savedProduct = productRepository.save(product);
        Long productId = savedProduct.getId();

        String idempotencyKey = "IDEM-KEY-" + UUID.randomUUID();
        System.out.println("🔑 Idempotency Key : " + idempotencyKey);
        System.out.println("📦 Sisa Stok Awal   : 10 unit\n");

        Order firstOrderResult = null;
        String firstOrderNumber = null;
        Long firstOrderId = null;

        // 2. Submit 5 identical requests with the SAME idempotency key
        for (int attempt = 1; attempt <= 5; attempt++) {
            CreateOrderRequest request = new CreateOrderRequest();
            request.setCustomerName("Andi Pratama");
            request.setCustomerEmail("andi@pratama.com");
            request.setPaymentMethod("QRIS");
            request.setIdempotencyKey(idempotencyKey);

            OrderItemDto itemDto = new OrderItemDto();
            itemDto.setProductId(productId);
            itemDto.setQuantity(2); // Buy 2 units
            request.setItems(List.of(itemDto));

            Order result = orderService.createOrder(request);
            assertNotNull(result, "Order result must not be null");

            if (attempt == 1) {
                firstOrderResult = result;
                firstOrderId = result.getId();
                firstOrderNumber = result.getOrderNumber();
                System.out.println("⚡ [ATTEMPT 1 - FIRST EXECUTION] Order dibuat baru! Invoice: " + firstOrderNumber);
            } else {
                System.out.println("🔄 [ATTEMPT " + attempt + " - CACHED HIT] Mengembalikan data invoice yang sama tanpa dobel deduct: " + result.getOrderNumber());
                assertEquals(firstOrderId, result.getId(), "Must return identical cached order ID");
                assertEquals(firstOrderNumber, result.getOrderNumber(), "Must return identical order number");
            }
        }

        // 3. Verifications
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        System.out.println("\n📊 HASIL VERIFIKASI IDEMPOTENCY:");
        System.out.println("========================================================");
        System.out.println("  • Total Request Dikirim   : 5 kali");
        System.out.println("  • Stok Awal               : 10 unit");
        System.out.println("  • Stok Akhir di DB        : " + updatedProduct.getStock() + " unit (Hanya terpotong 2 unit, BUKAN 10!)");
        
        List<StockMovement> movements = stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId);
        long saleMovements = movements.stream().filter(m -> "SALE".equals(m.getType())).count();
        System.out.println("  • Audit Ledger SALE       : " + saleMovements + " record");
        System.out.println("  • Idempotency Status      : ✅ 100% IDEMPOTENT (NO DOUBLE DEDUCTION)");
        System.out.println("========================================================\n");

        assertEquals(8, updatedProduct.getStock(), "Final product stock must be 8 (10 - 2)");
        assertEquals(1, saleMovements, "There must be EXACTLY 1 SALE stock movement recorded despite 5 submits");
    }
}
