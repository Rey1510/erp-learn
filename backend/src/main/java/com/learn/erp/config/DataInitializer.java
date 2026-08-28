package com.learn.erp.config;

import com.learn.erp.model.Order;
import com.learn.erp.model.OrderItem;
import com.learn.erp.model.Product;
import com.learn.erp.model.StockMovement;
import com.learn.erp.repository.OrderRepository;
import com.learn.erp.repository.ProductRepository;
import com.learn.erp.repository.StockMovementRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import com.learn.erp.model.User;
import com.learn.erp.repository.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    @Transactional
    CommandLineRunner initDatabase(
            ProductRepository productRepo, 
            OrderRepository orderRepo,
            StockMovementRepository stockMovementRepo,
            UserRepository userRepo,
            EntityManager entityManager) {
        return args -> {
            // 0. Ensure order_items.product_id is nullable in PostgreSQL for graceful product deletion
            try {
                entityManager.createNativeQuery("ALTER TABLE order_items ALTER COLUMN product_id DROP NOT NULL").executeUpdate();
            } catch (Exception ignored) {
                // Already dropped or handled
            }

            // Seed Users (Admin & Cashier)
            if (!userRepo.existsByEmail("admin@mail.com")) {
                userRepo.save(new User("Manager Toko", "admin@mail.com", "admin123", "ADMIN"));
                System.out.println(">>> [DataInitializer] Admin user (admin@mail.com) seeded!");
            }
            if (!userRepo.existsByEmail("cashier@mail.com")) {
                userRepo.save(new User("Kasir 01", "cashier@mail.com", "cashier123", "CASHIER"));
                System.out.println(">>> [DataInitializer] Cashier user (cashier@mail.com) seeded!");
            }

            // 1. Seed 20+ Enterprise Products across diverse categories
            if (!productRepo.existsBySku("AUD-SNY-20")) {
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

                for (Product p : catalog) {
                    if (!productRepo.existsBySku(p.getSku())) {
                        productRepo.save(p);
                    }
                }
                System.out.println(">>> [DataInitializer] 20+ Enterprise Products seeded into PostgreSQL successfully!");
            }

            // 2. Seed Initial Audit Stock Logs if empty
            if (stockMovementRepo.count() == 0) {
                List<Product> products = productRepo.findAll();
                LocalDateTime now = LocalDateTime.now();
                for (Product p : products) {
                    // Initial Setup log
                    StockMovement initLog = new StockMovement(
                            p.getId(), p.getName(), p.getSku(), "INITIAL",
                            p.getStock(), p.getStock(), "SYS-INIT",
                            "Setup awal stok katalog produk", now.minusDays(40)
                    );
                    stockMovementRepo.save(initLog);

                    // Add a sample restock log for popular items
                    if (p.getSku().equals("LAP-MBP-01") || p.getSku().equals("ACC-LOG-02")) {
                        StockMovement restockLog = new StockMovement(
                                p.getId(), p.getName(), p.getSku(), "RESTOCK",
                                10, p.getStock(), "PO-SUPPLIER-882",
                                "Pengiriman batch restock dari distributor utama", now.minusDays(12)
                        );
                        stockMovementRepo.save(restockLog);
                    }
                }
                System.out.println(">>> [DataInitializer] Initial Stock Movements & Audit Trail seeded successfully!");
            }

            // 3. Seed Diverse Multi-Date Sample Orders
            if (!orderRepo.existsByOrderNumber("ORD-20260722-0008")) {
                orderRepo.deleteAll(); // Refresh with rich multi-date seed
                List<Product> products = productRepo.findAll();
                if (!products.isEmpty()) {
                    Product macbook = products.get(0);
                    Product mouse = products.size() > 5 ? products.get(5) : macbook;
                    Product monitor = products.size() > 1 ? products.get(1) : macbook;
                    Product keyboard = products.size() > 6 ? products.get(6) : mouse;
                    Product desk = products.size() > 10 ? products.get(10) : mouse;

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

                    // T-35 (>30 Days Ago, for Testing 'ALL' vs '30D' Filter)
                    Order o8 = new Order("ORD-20260722-0008", "PT Sumber Rejeki Makmur", "procurement@sumberrejeki.com", "PAID", now.minusDays(35).minusHours(2));
                    o8.addItem(new OrderItem(macbook, 2));
                    o8.addItem(new OrderItem(mouse, 5));
                    o8.addItem(new OrderItem(monitor, 2));

                    orderRepo.saveAll(List.of(o1, o2, o3, o4, o5, o6, o7, o8));
                    System.out.println(">>> [DataInitializer] Diverse multi-date sample orders seeded successfully into PostgreSQL!");
                }
            }
        };
    }
}
