package com.learn.erp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.erp.dto.CreateOrderRequest;
import com.learn.erp.dto.OrderItemDto;
import com.learn.erp.dto.PageResponse;
import com.learn.erp.exception.InsufficientStockException;
import com.learn.erp.model.IdempotencyRecord;
import com.learn.erp.model.Order;
import com.learn.erp.model.OrderItem;
import com.learn.erp.model.Payment;
import com.learn.erp.model.Product;
import com.learn.erp.model.StockMovement;
import com.learn.erp.repository.IdempotencyRepository;
import com.learn.erp.repository.OrderRepository;
import com.learn.erp.repository.PaymentRepository;
import com.learn.erp.repository.ProductRepository;
import com.learn.erp.repository.StockMovementRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    @PersistenceContext
    private final EntityManager entityManager;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PaymentRepository paymentRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final StockMovementService stockMovementService;
    private final IdempotencyService idempotencyService;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @Autowired
    public OrderService(EntityManager entityManager,
                        OrderRepository orderRepository, 
                        ProductRepository productRepository, 
                        StockMovementRepository stockMovementRepository,
                        PaymentRepository paymentRepository,
                        IdempotencyRepository idempotencyRepository,
                        StockMovementService stockMovementService,
                        IdempotencyService idempotencyService,
                        @Lazy PaymentService paymentService,
                        ObjectMapper objectMapper) {
        this.entityManager = entityManager;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.paymentRepository = paymentRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.stockMovementService = stockMovementService;
        this.idempotencyService = idempotencyService;
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public PageResponse<Order> getPaginatedOrders(
            int page,
            int size,
            String sortBy,
            String direction,
            String search,
            String status) {

        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);

        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate orderNum = cb.like(cb.lower(root.get("orderNumber")), pattern);
                Predicate custName = cb.like(cb.lower(root.get("customerName")), pattern);
                Predicate custEmail = cb.like(cb.lower(cb.coalesce(root.get("customerEmail"), "")), pattern);
                predicates.add(cb.or(orderNum, custName, custEmail));
            }

            if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        return new PageResponse<>(orderPage);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order items tidak boleh kosong.");
        }

        String idemKey = request.getIdempotencyKey();
        if (idemKey != null && !idemKey.isBlank()) {
            Optional<IdempotencyRecord> existing = idempotencyService.findRecord(idemKey);
            if (existing.isPresent()) {
                IdempotencyRecord rec = existing.get();
                if ("COMPLETED".equals(rec.getStatus())) {
                    try {
                        return objectMapper.readValue(rec.getResponseBody(), Order.class);
                    } catch (Exception e) {
                        // Fallback
                    }
                } else if ("PENDING".equals(rec.getStatus())) {
                    throw new IllegalStateException("Permintaan checkout dengan key ini sedang diproses. Mohon tunggu sejenak.");
                }
            }
            String hash = idempotencyService.computeHash(request);
            idempotencyService.startRequest(idemKey, "/api/orders", hash);
        }

        try {
            // 1. Generate Unique Order Number with short random suffix to prevent collision
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String shortId = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            String orderNumber = "ORD-" + timestamp + "-" + shortId;

            String payMethod = (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank())
                    ? request.getPaymentMethod().toUpperCase()
                    : "CASH";

            String initialStatus = "CASH".equals(payMethod) ? "PAID" : "PENDING";
            Order order = new Order(orderNumber, request.getCustomerName(), request.getCustomerEmail(), initialStatus, payMethod);

            // 2. Sort items by productId ASC to prevent circular wait deadlocks during concurrent checkout
            List<OrderItemDto> sortedItems = new ArrayList<>(request.getItems());
            sortedItems.sort(Comparator.comparing(OrderItemDto::getProductId));

            // 3. Acquire pessimistic lock per product, check stock, deduct stock, and log movement
            for (OrderItemDto itemDto : sortedItems) {
                Product product = productRepository.findByIdWithLock(itemDto.getProductId())
                        .orElseThrow(() -> new RuntimeException("Produk dengan ID " + itemDto.getProductId() + " tidak ditemukan."));

                if (product.getStock() < itemDto.getQuantity()) {
                    throw new InsufficientStockException(
                            product.getId(),
                            product.getName(),
                            product.getStock(),
                            itemDto.getQuantity()
                    );
                }

                // Deduct product stock
                int newStock = product.getStock() - itemDto.getQuantity();
                product.setStock(newStock);
                product.setStatus(Product.calculateStatus(newStock));
                productRepository.save(product);

                // Audit Log: Record Stock Movement for Sale
                stockMovementService.logMovement(
                        product, 
                        "SALE", 
                        -itemDto.getQuantity(), 
                        orderNumber, 
                        "Penjualan POS (" + request.getCustomerName() + ")"
                );

                // Add item to Order
                OrderItem orderItem = new OrderItem(product, itemDto.getQuantity());
                order.addItem(orderItem);
            }

            // 4. Save Order (Cascading automatically saves all OrderItems)
            Order savedOrder = orderRepository.save(order);

            // 5. Initialize Payment Record
            paymentService.initiatePayment(savedOrder, payMethod);

            // 6. Complete Idempotency Record if key was present
            if (idemKey != null && !idemKey.isBlank()) {
                idempotencyService.completeRequest(idemKey, 201, savedOrder);
            }

            return savedOrder;
        } catch (Exception e) {
            if (idemKey != null && !idemKey.isBlank()) {
                idempotencyService.failRequest(idemKey);
            }
            throw e;
        }
    }

    @Transactional
    public Order updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order tidak ditemukan dengan id: " + id));

        // If cancelled and previously wasn't cancelled, restore stock & log movement with lock
        if ("CANCELLED".equalsIgnoreCase(status) && !"CANCELLED".equalsIgnoreCase(order.getStatus())) {
            List<OrderItem> items = new ArrayList<>(order.getItems());
            items.sort(Comparator.comparing(i -> i.getProduct().getId()));

            for (OrderItem item : items) {
                Product product = productRepository.findByIdWithLock(item.getProduct().getId())
                        .orElse(item.getProduct());

                int restoredStock = product.getStock() + item.getQuantity();
                product.setStock(restoredStock);
                product.setStatus(Product.calculateStatus(restoredStock));
                productRepository.save(product);

                // Audit Log: Record Stock Movement for Cancel Restock
                stockMovementService.logMovement(
                        product, 
                        "CANCEL_RESTOCK", 
                        item.getQuantity(), 
                        order.getOrderNumber(), 
                        "Pengembalian stok dari pembatalan transaksi"
                );
            }
        }

        order.setStatus(status.toUpperCase());
        return orderRepository.save(order);
    }

    @Transactional
    public List<Order> reseedSampleOrders() {
        // 1. CLEANSE ALL TRANSACTION & INVENTORY DATA (Native SQL in strict dependency order)
        entityManager.createNativeQuery("DELETE FROM payments").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM idempotency_records").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM order_items").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM orders").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM stock_movements").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM products").executeUpdate();
        entityManager.flush();
        entityManager.clear();

        // 2. SEED CLEAN MASTER PRODUCT CATALOG
        List<Product> catalog = List.of(
                // Electronics
                new Product("MacBook Pro M3 Max 16\"", "LAP-MBP-01", "Electronics", 38999000.0, 14),
                new Product("Dell UltraSharp 27\" 4K", "MON-DEL-03", "Electronics", 8250000.0, 8),
                new Product("iPad Pro 11\" M4 OLED", "TAB-APP-06", "Electronics", 17499000.0, 12),
                new Product("ThinkPad X1 Carbon Gen 11", "LAP-LEN-07", "Electronics", 29500000.0, 6),
                new Product("Samsung Odyssey Neo G9 49\"", "MON-SAM-08", "Electronics", 21500000.0, 3),

                // Accessories
                new Product("Logitech MX Master 3S", "ACC-LOG-02", "Accessories", 1650000.0, 25),
                new Product("Keychron Q1 Pro Wireless", "KEY-KCR-04", "Accessories", 2890000.0, 18),
                new Product("Anker 737 Power Bank 140W", "ACC-ANK-09", "Accessories", 1850000.0, 30),
                new Product("CalDigit TS4 Thunderbolt 4 Dock", "ACC-CDG-10", "Accessories", 6200000.0, 7),
                new Product("NuPhy Air75 V2 Low-Profile", "KEY-NUP-11", "Accessories", 1950000.0, 15),

                // Furniture
                new Product("Ergonomic Standing Desk 160x80", "FRN-DSK-05", "Furniture", 5400000.0, 10),
                new Product("Herman Miller Aeron Chair", "FRN-HMA-12", "Furniture", 22500000.0, 5),
                new Product("Steelcase Gesture Office Chair", "FRN-STC-13", "Furniture", 18900000.0, 4),
                new Product("Dual Monitor Heavy Duty Arm", "FRN-ARM-14", "Furniture", 1250000.0, 20),
                new Product("Acoustic Felt Desk Partition", "FRN-PRT-15", "Furniture", 850000.0, 12),

                // Audio
                new Product("Sony WH-1000XM5 ANC Headphones", "AUD-SNY-16", "Audio", 4999000.0, 16),
                new Product("Shure SM7B Dynamic Microphone", "AUD-SHR-17", "Audio", 6450000.0, 9),
                new Product("Audioengine A2+ Wireless Speakers", "AUD-AEN-18", "Audio", 4350000.0, 8),
                new Product("Rodecaster Pro II Audio Console", "AUD-RDE-19", "Audio", 10800000.0, 4),
                new Product("Sennheiser HD 660S2 Open-Back", "AUD-SNY-20", "Audio", 7890000.0, 5),

                // Stationery & Office Supplies
                new Product("Rhode Leather Desk Mat 90x40", "STN-MAT-21", "Stationery", 450000.0, 40),
                new Product("Rotring 600 Mechanical Pencil", "STN-RTR-22", "Stationery", 420000.0, 50),
                new Product("Leuchtturm1917 Hardcover Notebook", "STN-LCH-23", "Stationery", 320000.0, 35)
        );

        List<Product> savedProducts = productRepository.saveAll(catalog);

        // 3. SEED INITIAL STOCK AUDIT LOGS
        LocalDateTime now = LocalDateTime.now();
        for (Product p : savedProducts) {
            StockMovement initLog = new StockMovement(
                    p.getId(), p.getName(), p.getSku(), "INITIAL",
                    p.getStock(), p.getStock(), "SYS-INIT",
                    "Setup awal stok katalog produk", now.minusDays(40)
            );
            stockMovementRepository.save(initLog);

            if (p.getSku().equals("LAP-MBP-01") || p.getSku().equals("ACC-LOG-02")) {
                StockMovement restockLog = new StockMovement(
                        p.getId(), p.getName(), p.getSku(), "RESTOCK",
                        10, p.getStock(), "PO-SUPPLIER-882",
                        "Pengiriman batch restock dari distributor utama", now.minusDays(12)
                );
                stockMovementRepository.save(restockLog);
            }
        }

        // 4. SEED SAMPLE DIVERSE ORDERS (T-0 to T-35)
        Product macbook = savedProducts.get(0);
        Product mouse = savedProducts.size() > 5 ? savedProducts.get(5) : macbook;
        Product monitor = savedProducts.size() > 1 ? savedProducts.get(1) : macbook;
        Product keyboard = savedProducts.size() > 6 ? savedProducts.get(6) : mouse;
        Product desk = savedProducts.size() > 10 ? savedProducts.get(10) : mouse;

        // T-0 Active (3 Mins Ago) - ACTIVE PENDING VA (12 Minutes Remaining)
        Order o0 = new Order("ORD-20260829-0000", "Rian Hidayat", "rian@hidayat.com", "PENDING", now.minusMinutes(3));
        o0.setPaymentMethod("BANK_TRANSFER_VA");
        o0.setPaymentRef("880098290000");
        o0.addItem(new OrderItem(macbook, 1));

        // T-0 (Today - 2 Hours Ago) - CASH PAID
        Order o1 = new Order("ORD-20260826-0001", "Budi Santoso", "budi@mandiri.co.id", "PAID", now.minusHours(2));
        o1.setPaymentMethod("CASH");
        o1.setPaymentRef("CSH-POS-01");
        o1.addItem(new OrderItem(macbook, 1));
        o1.addItem(new OrderItem(mouse, 2));

        // T-2 (2 Days Ago) - QRIS
        Order o2 = new Order("ORD-20260824-0002", "Siti Aminah", "siti@mandiri.co.id", "PAID", now.minusDays(2).minusHours(3));
        o2.setPaymentMethod("QRIS");
        o2.setPaymentRef("QRIS.ID.08240002");
        o2.addItem(new OrderItem(keyboard, 2));
        o2.addItem(new OrderItem(mouse, 1));

        // T-4 (4 Days Ago) - BANK_TRANSFER_VA
        Order o3 = new Order("ORD-20260822-0003", "PT Digital Kreasi", "finance@kreasi.id", "PAID", now.minusDays(4).minusHours(5));
        o3.setPaymentMethod("BANK_TRANSFER_VA");
        o3.setPaymentRef("880098220003");
        o3.addItem(new OrderItem(monitor, 2));
        o3.addItem(new OrderItem(desk, 1));

        // T-6 (6 Days Ago) - EXPIRED / CANCELLED VA (Past 15 Minutes Validity)
        Order o4 = new Order("ORD-20260820-0004", "PT Solusi Jaya", "finance@solusijaya.co.id", "CANCELLED", now.minusDays(6).minusHours(1));
        o4.setPaymentMethod("BANK_TRANSFER_VA");
        o4.setPaymentRef("880098200004");
        o4.addItem(new OrderItem(macbook, 1));

        // T-10 (10 Days Ago) - CREDIT_CARD
        Order o5 = new Order("ORD-20260816-0005", "Mega Pratama", "mega@pratama.co.id", "PAID", now.minusDays(10).minusHours(4));
        o5.setPaymentMethod("CREDIT_CARD");
        o5.setPaymentRef("AUTH-CC-816005");
        o5.addItem(new OrderItem(keyboard, 3));

        // T-15 (15 Days Ago) - CASH
        Order o6 = new Order("ORD-20260811-0006", "Hendra Wijaya", "hendra@wijaya.com", "PAID", now.minusDays(15).minusHours(6));
        o6.setPaymentMethod("CASH");
        o6.setPaymentRef("CSH-POS-02");
        o6.addItem(new OrderItem(desk, 2));

        // T-20 (20 Days Ago) - CANCELLED
        Order o7 = new Order("ORD-20260806-0007", "Amanda Putri", "amanda@putri.id", "CANCELLED", now.minusDays(20).minusHours(8));
        o7.setPaymentMethod("QRIS");
        o7.setPaymentRef("QRIS.ID.08060007");
        o7.addItem(new OrderItem(monitor, 1));

        // T-35 (35 Days Ago) - BANK_TRANSFER_VA
        Order o8 = new Order("ORD-20260722-0008", "PT Sumber Rejeki Makmur", "procurement@sumberrejeki.com", "PAID", now.minusDays(35).minusHours(2));
        o8.setPaymentMethod("BANK_TRANSFER_VA");
        o8.setPaymentRef("880097220008");
        o8.addItem(new OrderItem(macbook, 2));
        o8.addItem(new OrderItem(mouse, 5));
        o8.addItem(new OrderItem(monitor, 2));

        List<Order> savedOrders = orderRepository.saveAll(List.of(o0, o1, o2, o3, o4, o5, o6, o7, o8));

        // 5. SEED PAYMENT RECORDS FOR ALL ORDERS
        for (Order o : savedOrders) {
            String payStatus = "PAID".equals(o.getStatus()) ? "SETTLED" : ("CANCELLED".equals(o.getStatus()) ? "EXPIRED" : "PENDING");
            Payment pay = new Payment(
                    "PAY-" + o.getOrderNumber(),
                    o.getId(),
                    o.getOrderNumber(),
                    o.getCustomerName(),
                    o.getTotalAmount(),
                    o.getPaymentMethod(),
                    payStatus,
                    o.getPaymentRef(),
                    o.getCreatedAt().plusMinutes(15)
            );
            if ("PAID".equals(o.getStatus())) {
                pay.setPaidAt(o.getCreatedAt());
            }
            pay.setCreatedAt(o.getCreatedAt());
            paymentRepository.save(pay);
        }

        return savedOrders;
    }

    /**
     * Demo Environment Auto-Reseed Job
     * Periodically resets database to pristine initial state every 2 hours.
     */
    @Scheduled(cron = "${erp.demo.auto-reset.cron:0 0 */2 * * *}")
    @Transactional
    public void scheduledAutoReseed() {
        System.out.println(">>> [DemoAutoReset] Running scheduled 2-hour demo database reset & reseed...");
        reseedSampleOrders();
        System.out.println(">>> [DemoAutoReset] Database successfully reset to pristine demo state!");
    }
}
