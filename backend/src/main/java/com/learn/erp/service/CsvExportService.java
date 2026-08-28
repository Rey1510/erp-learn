package com.learn.erp.service;

import com.learn.erp.model.Order;
import com.learn.erp.model.Product;
import com.learn.erp.repository.OrderRepository;
import com.learn.erp.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CsvExportService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Autowired
    public CsvExportService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public byte[] exportProductsToCsv() {
        List<Product> products = productRepository.findAll();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            // Write UTF-8 BOM for Microsoft Excel compatibility
            writer.write('\uFEFF');

            // Header
            writer.println("SKU,Nama Produk,Kategori,Harga (IDR),Stok,Status");

            for (Product p : products) {
                writer.printf("%s,%s,%s,%.0f,%d,%s%n",
                        escapeCsv(p.getSku()),
                        escapeCsv(p.getName()),
                        escapeCsv(p.getCategory()),
                        p.getPrice() != null ? p.getPrice() : 0.0,
                        p.getStock() != null ? p.getStock() : 0,
                        escapeCsv(p.getStatus())
                );
            }
            writer.flush();
        }

        return out.toByteArray();
    }

    public byte[] exportOrdersToCsv() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            // Write UTF-8 BOM for Microsoft Excel compatibility
            writer.write('\uFEFF');

            // Header
            writer.println("No. Order,Customer,Email,Tanggal,Jumlah Produk,Total Tagihan (IDR),Status");

            for (Order o : orders) {
                String dateStr = o.getCreatedAt() != null ? o.getCreatedAt().format(dtf) : "-";
                int itemCount = o.getItems() != null ? o.getItems().size() : 0;
                double total = o.getTotalAmount();

                writer.printf("%s,%s,%s,%s,%d,%.0f,%s%n",
                        escapeCsv(o.getOrderNumber()),
                        escapeCsv(o.getCustomerName()),
                        escapeCsv(o.getCustomerEmail() != null ? o.getCustomerEmail() : ""),
                        escapeCsv(dateStr),
                        itemCount,
                        total,
                        escapeCsv(o.getStatus())
                );
            }
            writer.flush();
        }

        return out.toByteArray();
    }

    private String escapeCsv(String val) {
        if (val == null) return "\"\"";
        String escaped = val.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return "\"" + escaped + "\"";
    }
}
