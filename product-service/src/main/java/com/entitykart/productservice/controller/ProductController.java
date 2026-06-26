package com.entitykart.productservice.controller;

import com.entitykart.productservice.dto.ProductDTO;
import com.entitykart.productservice.service.ProductService;
import com.entitykart.productservice.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CloudinaryService cloudinaryService;

    @PostMapping
    public ProductDTO createProduct(@RequestBody ProductDTO productDTO) {
        return productService.createProduct(productDTO);
    }

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = cloudinaryService.uploadImage(file);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ProductDTO getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }

    // @GetMapping
    // public Page<ProductDTO> getProducts(
    //         @RequestParam(required = false) Long categoryId,
    //         @RequestParam(required = false) Long sellerId,
    //         Pageable pageable) {
    //     if (categoryId != null) {
    //         return productService.getProductsByCategory(categoryId, pageable);
    //     }
    //     if (sellerId != null) {
    //         return productService.getProductsBySeller(sellerId, pageable);
    //     }
    //     return productService.getProducts(pageable);
    // }

    @GetMapping
    public Page<ProductDTO> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subCategoryId,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            Pageable pageable) {
        if (sellerId != null) {
            return productService.getProductsBySeller(sellerId, pageable);
        }
        return productService.getProductsFiltered(categoryId, subCategoryId, search, minPrice, maxPrice, pageable);
    }

    @GetMapping("/all")
    public List<ProductDTO> getAllProducts() {
        return productService.getAllProducts();
    }

    @PutMapping("/{id}")
    public ProductDTO updateProduct(@PathVariable Long id, @RequestBody ProductDTO productDTO) {
        return productService.updateProduct(id, productDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }

    @GetMapping("/stats")
    public Map<String, Object> getProductStats() {
        return productService.getProductStats();
    }
}
