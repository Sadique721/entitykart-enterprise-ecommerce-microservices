package com.entitykart.productservice.controller;

import com.entitykart.productservice.dto.ProductDTO;
import com.entitykart.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ProductDTO createProduct(@RequestBody ProductDTO productDTO) {
        return productService.createProduct(productDTO);
    }

    @GetMapping("/{id}")
    public ProductDTO getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }

    @GetMapping
    public Page<ProductDTO> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long sellerId,
            Pageable pageable) {
        if (categoryId != null) {
            return productService.getProductsByCategory(categoryId, pageable);
        }
        if (sellerId != null) {
            return productService.getProductsBySeller(sellerId, pageable);
        }
        return productService.getProducts(pageable);
    }

    @GetMapping("/all")
    public List<ProductDTO> getAllProducts() {
        return productService.getAllProducts();
    }
}
