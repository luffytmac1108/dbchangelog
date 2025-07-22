package com.yxw.dbchangelog.controller;

import com.yxw.dbchangelog.model.Product;
import com.yxw.dbchangelog.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product createdProduct = productService.createProduct(product);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        if (product != null) {
            return ResponseEntity.ok(product);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * 更新产品信息，并触发异步日志记录（成功时）。
     * URL: POST /products/update/{id}?newPrice=...&newStock=...&updatedBy=...
     */
    @PostMapping("/update/{id}")
    public ResponseEntity<Map<String, String>> updateProduct(@PathVariable Long id, @RequestParam BigDecimal newPrice,
                                                             @RequestParam Integer newStock, @RequestParam String updatedBy) {
        Map<String, String> response = new HashMap<>();
        try {
            Product updatedProduct = productService.updateProductAndPrepareLog(id, newPrice, newStock, updatedBy);
            response.put("status", "success");
            response.put("message", "Product updated successfully. Log event published.");
            response.put("productId", updatedProduct.getId().toString());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", "Bad request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (RuntimeException e) {
            response.put("status", "error");
            response.put("message", "Failed to update product due to business logic: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 模拟更新失败的场景，用于测试事务回滚和日志不记录。
     * URL: POST /products/update-fail/{id}?newPrice=...&newStock=...&updatedBy=...
     */
    @PostMapping("/update-fail/{id}")
    public ResponseEntity<Map<String, String>> updateProductSimulatingFailure(@PathVariable Long id, @RequestParam BigDecimal newPrice,
                                                                              @RequestParam Integer newStock, @RequestParam String updatedBy) {
        Map<String, String> response = new HashMap<>();
        try {
            productService.updateProductAndPrepareLogSimulatingFailure(id, newPrice, newStock, updatedBy);
            response.put("status", "success");
            response.put("message", "Simulated success (should not happen).");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", "Bad request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (RuntimeException e) {
            response.put("status", "error");
            response.put("message", "Simulated failure: " + e.getMessage() + ". Log should NOT be recorded.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "An unexpected error occurred during simulated failure: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
