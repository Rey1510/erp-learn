package com.learn.erp.service;

import com.learn.erp.dto.CreateOrderRequest;
import com.learn.erp.dto.OrderItemDto;
import com.learn.erp.dto.PageResponse;
import com.learn.erp.model.Order;
import com.learn.erp.model.OrderItem;
import com.learn.erp.model.Product;
import com.learn.erp.repository.OrderRepository;
import com.learn.erp.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;

    @Autowired
    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, StockMovementService stockMovementService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.stockMovementService = stockMovementService;
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

        // 1. Generate Unique Order Number
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String orderNumber = "ORD-" + timestamp;

        Order order = new Order(orderNumber, request.getCustomerName(), request.getCustomerEmail(), "PENDING");

        // 2. Loop items, check stock, deduct stock, log movement, and create OrderItem
        for (OrderItemDto itemDto : request.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Produk dengan ID " + itemDto.getProductId() + " tidak ditemukan."));

            if (product.getStock() < itemDto.getQuantity()) {
                throw new IllegalStateException("Stok produk '" + product.getName() + "' tidak mencukupi. Sisa stok: " + product.getStock());
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

        // 3. Save Order (Cascading automatically saves all OrderItems)
        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order tidak ditemukan dengan id: " + id));

        // If cancelled and previously wasn't cancelled, restore stock & log movement
        if ("CANCELLED".equalsIgnoreCase(status) && !"CANCELLED".equalsIgnoreCase(order.getStatus())) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
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
        orderRepository.deleteAll();
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return List.of();
        }

        Product macbook = products.get(0);
        Product mouse = products.size() > 1 ? products.get(1) : macbook;
        Product monitor = products.size() > 2 ? products.get(2) : macbook;
        Product keyboard = products.size() > 3 ? products.get(3) : mouse;
        Product desk = products.size() > 4 ? products.get(4) : mouse;

        LocalDateTime now = LocalDateTime.now();

        // T-0 (Today)
        Order o1 = new Order("ORD-20260826-0001", "Budi Santoso", "budi@mandiri.co.id", "PAID", now.minusHours(2));
        o1.addItem(new OrderItem(macbook, 1));
        o1.addItem(new OrderItem(mouse, 2));

        // T-2 (2 Days Ago)
        Order o2 = new Order("ORD-20260824-0002", "Siti Aminah", "siti@mandiri.co.id", "PAID", now.minusDays(2).minusHours(3));
        o2.addItem(new OrderItem(keyboard, 2));
        o2.addItem(new OrderItem(mouse, 1));

        // T-4 (4 Days Ago)
        Order o3 = new Order("ORD-20260822-0003", "PT Digital Kreasi", "finance@kreasi.id", "PAID", now.minusDays(4).minusHours(5));
        o3.addItem(new OrderItem(monitor, 2));
        o3.addItem(new OrderItem(desk, 1));

        // T-6 (6 Days Ago)
        Order o4 = new Order("ORD-20260820-0004", "Rian Hidayat", "rian@hidayat.com", "PENDING", now.minusDays(6).minusHours(1));
        o4.addItem(new OrderItem(macbook, 1));

        // T-10 (10 Days Ago)
        Order o5 = new Order("ORD-20260816-0005", "Mega Pratama", "mega@pratama.co.id", "PAID", now.minusDays(10).minusHours(4));
        o5.addItem(new OrderItem(keyboard, 3));

        // T-15 (15 Days Ago)
        Order o6 = new Order("ORD-20260811-0006", "Hendra Wijaya", "hendra@wijaya.com", "PAID", now.minusDays(15).minusHours(6));
        o6.addItem(new OrderItem(desk, 2));

        // T-20 (20 Days Ago)
        Order o7 = new Order("ORD-20260806-0007", "Amanda Putri", "amanda@putri.id", "CANCELLED", now.minusDays(20).minusHours(8));
        o7.addItem(new OrderItem(monitor, 1));

        // T-35 (35 Days Ago)
        Order o8 = new Order("ORD-20260722-0008", "PT Sumber Rejeki Makmur", "procurement@sumberrejeki.com", "PAID", now.minusDays(35).minusHours(2));
        o8.addItem(new OrderItem(macbook, 2));
        o8.addItem(new OrderItem(mouse, 5));
        o8.addItem(new OrderItem(monitor, 2));

        return orderRepository.saveAll(List.of(o1, o2, o3, o4, o5, o6, o7, o8));
    }
}
