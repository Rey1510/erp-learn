package com.learn.erp.controller;

import com.learn.erp.service.CsvExportService;
import com.learn.erp.service.CsvImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001"})
public class ExportImportController {

    private final CsvExportService csvExportService;
    private final CsvImportService csvImportService;

    @Autowired
    public ExportImportController(CsvExportService csvExportService, CsvImportService csvImportService) {
        this.csvExportService = csvExportService;
        this.csvImportService = csvImportService;
    }

    @GetMapping("/export/products.csv")
    public ResponseEntity<byte[]> exportProductsCsv() {
        byte[] csvData = csvExportService.exportProductsToCsv();
        String filename = "katalog_produk_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping("/export/orders.csv")
    public ResponseEntity<byte[]> exportOrdersCsv() {
        byte[] csvData = csvExportService.exportOrdersToCsv();
        String filename = "laporan_penjualan_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @PostMapping(value = "/import/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importProductsCsv(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = csvImportService.importProductsFromCsv(file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Gagal memproses file import: " + e.getMessage()));
        }
    }
}
