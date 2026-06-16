package com.entitykart.productservice.service;

import com.entitykart.productservice.dto.ProductDTO;
import com.entitykart.productservice.entity.ProductEntity;
import com.entitykart.productservice.event.ProductCreatedEvent;
import com.entitykart.productservice.exception.ProductNotFoundException;
import com.entitykart.productservice.repository.ProductRepository;
import com.entitykart.productservice.repository.CategoryRepository;
import com.entitykart.productservice.repository.SubCategoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String PRODUCT_EVENTS_TOPIC = "product-events";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public ProductDTO createProduct(ProductDTO dto) {
        ProductEntity product = new ProductEntity();
        product.setProductName(dto.getProductName());
        product.setDescription(dto.getDescription());
        product.setBrand(dto.getBrand());
        product.setPrice(dto.getPrice());
        product.setMrp(dto.getMrp());
        product.setStockQuantity(dto.getStockQuantity());
        product.setSku(dto.getSku());
        product.setMainImageURL(dto.getMainImageURL());
        product.setCategoryId(dto.getCategoryId());
        product.setSubCategoryId(dto.getSubCategoryId());
        product.setSellerId(dto.getSellerId());
        if (dto.getStatus() != null) {
            product.setStatus(dto.getStatus());
        }

        ProductEntity saved = productRepository.save(product);

        ProductCreatedEvent event = new ProductCreatedEvent(
                saved.getProductId(),
                saved.getProductName(),
                saved.getSellerId(),
                LocalDateTime.now());
        kafkaTemplate.send(PRODUCT_EVENTS_TOPIC, event);

        return convertToDTO(saved);
    }

    @Transactional(readOnly = true)
    public ProductDTO getProduct(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
        return convertToDTO(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getProductsBySeller(Long sellerId, Pageable pageable) {
        return productRepository.findBySellerId(sellerId, pageable).map(this::convertToDTO);
    }

    private ProductDTO convertToDTO(ProductEntity entity) {
        ProductDTO dto = new ProductDTO();
        dto.setProductId(entity.getProductId());
        dto.setProductName(entity.getProductName());
        dto.setDescription(entity.getDescription());
        dto.setBrand(entity.getBrand());
        dto.setPrice(entity.getPrice());
        dto.setMrp(entity.getMrp());
        dto.setStockQuantity(entity.getStockQuantity());
        dto.setSku(entity.getSku());
        dto.setMainImageURL(entity.getMainImageURL());
        dto.setCategoryId(entity.getCategoryId());
        dto.setSubCategoryId(entity.getSubCategoryId());
        dto.setSellerId(entity.getSellerId());
        dto.setDiscountPercent(entity.getDiscountPercent());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO dto) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
        product.setProductName(dto.getProductName());
        product.setDescription(dto.getDescription());
        product.setBrand(dto.getBrand());
        product.setPrice(dto.getPrice());
        product.setMrp(dto.getMrp());
        product.setStockQuantity(dto.getStockQuantity());
        product.setSku(dto.getSku());
        product.setMainImageURL(dto.getMainImageURL());
        product.setCategoryId(dto.getCategoryId());
        product.setSubCategoryId(dto.getSubCategoryId());
        product.setSellerId(dto.getSellerId());
        if (dto.getStatus() != null) {
            product.setStatus(dto.getStatus());
        }
        ProductEntity saved = productRepository.save(product);
        return convertToDTO(saved);
    }

    @Transactional
    public void deleteProduct(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getProductStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalProducts", productRepository.count());
        stats.put("totalCategories", categoryRepository.count());
        stats.put("totalSubCategories", subCategoryRepository.count());
        return stats;
    }
}
