package com.learn.erp.service;

import com.learn.erp.model.Product;
import com.learn.erp.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class CsvImportService {

    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;

    @Autowired
    public CsvImportService(ProductRepository productRepository, StockMovementService stockMovementService) {
        this.productRepository = productRepository;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    public Map<String, Object> importProductsFromCsv(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File CSV tidak boleh kosong.");
        }

        int createdCount = 0;
        int updatedCount = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Strip UTF-8 BOM if present on the first line
                if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }

                line = line.trim();
                if (line.isEmpty()) continue;

                // Skip header row
                if (lineNumber == 1 && (line.toLowerCase().contains("sku") || line.toLowerCase().contains("nama"))) {
                    continue;
                }

                List<String> tokens = parseCsvLine(line);
                if (tokens.size() < 4) {
                    errors.add("Baris " + lineNumber + ": Format kolom tidak lengkap (minimal 4 kolom: Nama, SKU, Kategori, Harga, [Stok]).");
                    continue;
                }

                try {
                    String name = tokens.get(0).trim();
                    String sku = tokens.get(1).trim();
                    String category = tokens.get(2).trim();
                    double price = Double.parseDouble(tokens.get(3).trim().replace(",", ""));
                    int stock = tokens.size() > 4 ? Integer.parseInt(tokens.get(4).trim()) : 0;

                    if (name.isEmpty() || sku.isEmpty()) {
                        errors.add("Baris " + lineNumber + ": Nama dan SKU tidak boleh kosong.");
                        continue;
                    }

                    if (price <= 0) {
                        errors.add("Baris " + lineNumber + ": Harga harus bernilai positif (> 0).");
                        continue;
                    }

                    if (stock < 0) {
                        errors.add("Baris " + lineNumber + ": Stok tidak boleh bernilai negatif (< 0).");
                        continue;
                    }

                    Optional<Product> existingOpt = productRepository.findBySku(sku);
                    if (existingOpt.isPresent()) {
                        Product existing = existingOpt.get();
                        existing.setName(name);
                        existing.setCategory(category.isEmpty() ? "Electronics" : category);
                        existing.setPrice(price);
                        productRepository.save(existing);
                        updatedCount++;
                    } else {
                        Product newProduct = new Product(name, sku, category.isEmpty() ? "Electronics" : category, price, Math.max(0, stock));
                        newProduct.setStatus(Product.calculateStatus(newProduct.getStock()));
                        Product saved = productRepository.save(newProduct);

                        // Audit Log: Record initial stock movement
                        if (saved.getStock() > 0) {
                            stockMovementService.logMovement(
                                    saved,
                                    "INITIAL",
                                    saved.getStock(),
                                    "CSV-IMPORT",
                                    "Batch import katalog dari file CSV"
                            );
                        }
                        createdCount++;
                    }
                } catch (NumberFormatException e) {
                    errors.add("Baris " + lineNumber + ": Format angka harga atau stok tidak valid (" + e.getMessage() + ").");
                } catch (Exception e) {
                    errors.add("Baris " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Gagal memproses file CSV: " + e.getMessage(), e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("created", createdCount);
        result.put("updated", updatedCount);
        result.put("totalProcessed", createdCount + updatedCount);
        result.put("errors", errors);
        return result;
    }

    private List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim().replaceAll("^\"|\"$", ""));
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim().replaceAll("^\"|\"$", ""));
        return tokens;
    }
}
