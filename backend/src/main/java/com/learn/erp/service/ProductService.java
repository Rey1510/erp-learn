package com.learn.erp.service;

import com.learn.erp.dto.PageResponse;
import com.learn.erp.model.Product;
import com.learn.erp.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public ProductService(ProductRepository productRepository, StockMovementService stockMovementService) {
        this.productRepository = productRepository;
        this.stockMovementService = stockMovementService;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public PageResponse<Product> getPaginatedProducts(
            int page,
            int size,
            String sortBy,
            String direction,
            String search,
            String category,
            String status) {

        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
                Predicate skuLike = cb.like(cb.lower(root.get("sku")), pattern);
                predicates.add(cb.or(nameLike, skuLike));
            }

            if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category.trim())) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }

            if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        return new PageResponse<>(productPage);
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional
    public Product createProduct(Product product) {
        product.setStatus(Product.calculateStatus(product.getStock()));
        Product saved = productRepository.save(product);

        // Audit Log: Record Initial Setup Stock
        if (saved.getStock() != null && saved.getStock() > 0) {
            stockMovementService.logMovement(
                    saved, 
                    "INITIAL", 
                    saved.getStock(), 
                    "PROD-INIT", 
                    "Setup awal stok produk baru"
            );
        }

        return saved;
    }

    @Transactional
    public Product updateProduct(Long id, Product updatedProduct) {
        return productRepository.findById(id).map(existing -> {
            existing.setName(updatedProduct.getName());
            existing.setSku(updatedProduct.getSku());
            existing.setCategory(updatedProduct.getCategory());
            existing.setPrice(updatedProduct.getPrice());
            // Stock is intentionally NOT overwritten here to preserve audit trail integrity
            return productRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        try {
            entityManager.createNativeQuery("ALTER TABLE order_items ALTER COLUMN product_id DROP NOT NULL").executeUpdate();
        } catch (Exception ignored) {}

        entityManager.createNativeQuery("UPDATE order_items SET product_id = NULL WHERE product_id = :id")
                .setParameter("id", id)
                .executeUpdate();

        productRepository.deleteById(id);
    }

    @Transactional
    public void deleteProductsByIds(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            try {
                entityManager.createNativeQuery("ALTER TABLE order_items ALTER COLUMN product_id DROP NOT NULL").executeUpdate();
            } catch (Exception ignored) {}

            entityManager.createNativeQuery("UPDATE order_items SET product_id = NULL WHERE product_id IN (:ids)")
                    .setParameter("ids", ids)
                    .executeUpdate();

            productRepository.deleteAllById(ids);
        }
    }
}
