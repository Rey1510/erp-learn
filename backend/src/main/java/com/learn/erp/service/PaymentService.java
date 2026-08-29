package com.learn.erp.service;

import com.learn.erp.model.Order;
import com.learn.erp.model.OrderItem;
import com.learn.erp.model.Payment;
import com.learn.erp.model.Product;
import com.learn.erp.repository.OrderRepository;
import com.learn.erp.repository.PaymentRepository;
import com.learn.erp.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, 
                          OrderRepository orderRepository, 
                          ProductRepository productRepository, 
                          StockMovementService stockMovementService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    public Payment initiatePayment(Order order, String method) {
        String payMethod = (method != null && !method.isBlank()) ? method.toUpperCase() : "CASH";
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String shortId = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String paymentNumber = "PAY-" + timestamp + "-" + shortId;

        String referenceNumber;
        switch (payMethod) {
            case "QRIS" -> referenceNumber = "QRIS.ID." + (10000000 + new Random().nextInt(90000000));
            case "BANK_TRANSFER_VA" -> referenceNumber = "88009" + (10000000 + new Random().nextInt(90000000));
            case "CREDIT_CARD" -> referenceNumber = "AUTH-" + (100000 + new Random().nextInt(900000));
            default -> referenceNumber = "CASH-" + shortId;
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        String initialStatus = "CASH".equals(payMethod) ? "SETTLED" : "PENDING";

        Payment payment = new Payment(
                paymentNumber,
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getTotalAmount(),
                payMethod,
                initialStatus,
                referenceNumber,
                expiresAt
        );

        if ("SETTLED".equals(initialStatus)) {
            payment.setPaidAt(LocalDateTime.now());
            order.setStatus("PAID");
        }

        order.setPaymentMethod(payMethod);
        order.setPaymentRef(referenceNumber);
        orderRepository.save(order);

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment simulateCallback(Long paymentId, String action, String notes) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment dengan ID " + paymentId + " tidak ditemukan."));

        if (!"PENDING".equalsIgnoreCase(payment.getStatus())) {
            throw new IllegalStateException("Payment sudah berstatus: " + payment.getStatus() + ", tidak dapat disimulasikan lagi.");
        }

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order terkait tidak ditemukan."));

        String act = action != null ? action.toUpperCase() : "SETTLE";

        if ("SETTLE".equals(act) || "SUCCESS".equals(act)) {
            payment.setStatus("SETTLED");
            payment.setPaidAt(LocalDateTime.now());
            payment.setNotes(notes != null ? notes : "Simulasi Pembayaran Berhasil oleh Gateway");
            order.setStatus("PAID");
            orderRepository.save(order);
        } else if ("EXPIRE".equals(act) || "TIMEOUT".equals(act)) {
            payment.setStatus("EXPIRED");
            payment.setNotes(notes != null ? notes : "Simulasi Payment Expired (Timeout 15 Menit)");
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            restoreStockForOrder(order, "Pengembalian stok dari pembayaran QRIS/VA yang expired");
        } else if ("FAIL".equals(act) || "DECLINED".equals(act)) {
            payment.setStatus("FAILED");
            payment.setNotes(notes != null ? notes : "Simulasi Transaksi Ditolak oleh Bank / Saldo Tidak Cukup");
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            restoreStockForOrder(order, "Pengembalian stok dari transaksi pembayaran yang gagal");
        } else {
            throw new IllegalArgumentException("Aksi simulasi tidak valid: " + action);
        }

        return paymentRepository.save(payment);
    }

    /**
     * Background Cron Worker: checks and automatically expires pending payments that have passed expiresAt.
     * Restores stock to catalog and cancels the order automatically.
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void autoExpirePendingPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<Payment> expiredPayments = paymentRepository.findByStatusAndExpiresAtBefore("PENDING", now);
        for (Payment payment : expiredPayments) {
            expireSinglePayment(payment, "Auto-expired oleh background engine: Waktu pembayaran 15 menit telah habis");
        }
    }

    @Transactional
    public void expireSinglePayment(Payment payment, String reason) {
        payment.setStatus("EXPIRED");
        payment.setNotes(reason);
        paymentRepository.save(payment);

        Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
        if (order != null && "PENDING".equalsIgnoreCase(order.getStatus())) {
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            restoreStockForOrder(order, reason);
        }
    }

    private void restoreStockForOrder(Order order, String reason) {
        List<OrderItem> items = new ArrayList<>(order.getItems());
        items.sort(Comparator.comparing(i -> i.getProduct().getId()));

        for (OrderItem item : items) {
            Product product = productRepository.findByIdWithLock(item.getProduct().getId())
                    .orElse(item.getProduct());

            int restoredStock = product.getStock() + item.getQuantity();
            product.setStock(restoredStock);
            product.setStatus(Product.calculateStatus(restoredStock));
            productRepository.save(product);

            stockMovementService.logMovement(
                    product,
                    "CANCEL_RESTOCK",
                    item.getQuantity(),
                    order.getOrderNumber(),
                    reason
            );
        }
    }

    @Transactional
    public Optional<Payment> getLatestPaymentForOrder(Long orderId) {
        Optional<Payment> optPayment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId);
        if (optPayment.isPresent()) {
            Payment payment = optPayment.get();
            if ("PENDING".equalsIgnoreCase(payment.getStatus()) && payment.getExpiresAt() != null && payment.getExpiresAt().isBefore(LocalDateTime.now())) {
                expireSinglePayment(payment, "Auto-expired pada saat akses: Waktu pembayaran 15 menit telah habis");
            }
        }
        return optPayment;
    }

    @Transactional
    public Optional<Payment> getPaymentById(Long id) {
        Optional<Payment> optPayment = paymentRepository.findById(id);
        if (optPayment.isPresent()) {
            Payment payment = optPayment.get();
            if ("PENDING".equalsIgnoreCase(payment.getStatus()) && payment.getExpiresAt() != null && payment.getExpiresAt().isBefore(LocalDateTime.now())) {
                expireSinglePayment(payment, "Auto-expired pada saat akses: Waktu pembayaran 15 menit telah habis");
            }
        }
        return optPayment;
    }
}
