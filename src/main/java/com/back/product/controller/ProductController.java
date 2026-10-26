package com.back.product.controller;

import com.back.product.entity.Product;
import com.back.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ==================== CREATE ====================
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody CreateRequest request) {
        return ResponseEntity.ok(productService.create(request.name(), request.keywords()));
    }

    // ==================== READ ====================
    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Product>> findAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    // ==================== UPDATE ====================
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody UpdateRequest request) {
        if (productService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(productService.update(id, request.name(), request.keywords()));
    }

    // ==================== DELETE ====================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== KNN SEARCH ====================
    @GetMapping("/search")
    public ResponseEntity<List<Product>> search(@RequestParam String query, @RequestParam(defaultValue = "5") int k) {
        return ResponseEntity.ok(productService.knnSearch(query, k));
    }

    @PostMapping("/search")
    public ResponseEntity<List<Product>> searchByKeywords(@RequestBody SearchRequest request) {
        return ResponseEntity.ok(productService.knnSearch(request.keywords(), request.k()));
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<List<Product>> findSimilarProducts(@PathVariable Long id, @RequestParam(defaultValue = "5") int k) {
        if (productService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(productService.findSimilarProducts(id, k));
    }

    // ==================== Request DTOs ====================
    public record CreateRequest(String name, List<String> keywords) {}
    public record UpdateRequest(String name, List<String> keywords) {}
    public record SearchRequest(List<String> keywords, int k) {
        public SearchRequest {
            if (k <= 0) k = 10; // k를 보내지 않으면 0 → 기본값 10
        }
    }
}
