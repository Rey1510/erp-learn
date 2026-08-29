package com.learn.erp.service;

import com.learn.erp.dto.CreateOrderRequest;
import com.learn.erp.dto.OrderItemDto;
import com.learn.erp.exception.InsufficientStockException;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderServiceConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Test
    @DisplayName("Should prevent overselling when multiple concurrent POS terminals buy the last remaining stock")
    void testConcurrentCheckoutOnSingleStockProduct() throws InterruptedException {
        System.out.println("\n========================================================");
        System.out.println("🔥 [CONCURRENCY TEST] Simulasi 10 Kasir Berebut 1 Sisa Stok");
        System.out.println("========================================================");

        // 1. Setup a test product with exactly 1 unit of stock
        String testSku = "TEST-CONCURRENT-" + UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product("iPhone 16 Pro Limited", testSku, "Electronics", 1500.0, 1);
        Product savedProduct = productRepository.save(product);
        Long productId = savedProduct.getId();

        System.out.println("📦 Produk disiapkan: " + product.getName() + " | SKU: " + testSku + " | Sisa Stok Awal = 1\n");

        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientStockCount = new AtomicInteger(0);
        AtomicInteger unexpectedErrorCount = new AtomicInteger(0);

        // 2. Spawn 10 simultaneous threads (simulating 10 cashiers clicking checkout at the exact same millisecond)
        for (int i = 0; i < numberOfThreads; i++) {
            final int cashierIndex = i + 1;
            executor.submit(() -> {
                try {
                    // Wait for all threads to align at the exact starting line
                    startLatch.await();

                    CreateOrderRequest request = new CreateOrderRequest();
                    request.setCustomerName("Kasir Terminal #" + cashierIndex);
                    request.setCustomerEmail("terminal" + cashierIndex + "@pos.com");

                    OrderItemDto itemDto = new OrderItemDto();
                    itemDto.setProductId(productId);
                    itemDto.setQuantity(1);
                    request.setItems(List.of(itemDto));

                    Order order = orderService.createOrder(request);
                    if (order != null && order.getId() != null) {
                        successCount.incrementAndGet();
                        System.out.println("✅ [BERHASIL] Kasir Terminal #" + cashierIndex + " memenangkan lock! Invoice: " + order.getOrderNumber());
                    }
                } catch (InsufficientStockException e) {
                    insufficientStockCount.incrementAndGet();
                    System.out.println("⛔ [DITOLAK DENGAN AMAN] Kasir Terminal #" + cashierIndex + " -> " + e.getMessage());
                } catch (Exception e) {
                    unexpectedErrorCount.incrementAndGet();
                    System.err.println("💥 [UNEXPECTED ERROR] Kasir Terminal #" + cashierIndex + ": " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        System.out.println("🚀 Menembakkan 10 request checkout secara serentak...");
        startLatch.countDown();
        boolean completedInTime = endLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        // 3. Verifications
        assertTrue(completedInTime, "Concurrency execution timed out");
        assertEquals(0, unexpectedErrorCount.get(), "No unexpected exceptions should occur");
        assertEquals(1, successCount.get(), "Exactly 1 order must succeed because stock was 1");
        assertEquals(numberOfThreads - 1, insufficientStockCount.get(), "All other concurrent requests must fail with InsufficientStockException");

        // 4. Verify Database Product State (Stock must be exactly 0, not negative!)
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertEquals(0, updatedProduct.getStock(), "Final product stock in database must be exactly 0");
        assertEquals("OUT_OF_STOCK", updatedProduct.getStatus(), "Status should be OUT_OF_STOCK");

        // 5. Verify Stock Movement Audit Trail
        List<StockMovement> movements = stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId);
        long saleMovements = movements.stream().filter(m -> "SALE".equals(m.getType())).count();
        assertEquals(1, saleMovements, "There must be exactly 1 SALE stock movement recorded");

        System.out.println("\n========================================================");
        System.out.println("📊 HASIL PENGUJIAN CONCURRENCY:");
        System.out.println("========================================================");
        System.out.println("  • Total Kasir Menembak   : " + numberOfThreads);
        System.out.println("  • Berhasil Checkout      : " + successCount.get() + " (Tepat 1 kasir)");
        System.out.println("  • Ditolak (Stok Habis)   : " + insufficientStockCount.get() + " (9 kasir dicegah overselling)");
        System.out.println("  • Sisa Stok di Database  : " + updatedProduct.getStock() + " (" + updatedProduct.getStatus() + ")");
        System.out.println("  • Audit Ledger SALE      : " + saleMovements + " record");
        System.out.println("  • Status Integrity       : ✅ 100% KONSISTEN (ZERO OVERSELLING)");
        System.out.println("========================================================\n");
    }
}
