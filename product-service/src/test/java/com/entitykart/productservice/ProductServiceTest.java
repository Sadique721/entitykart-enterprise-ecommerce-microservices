package com.entitykart.productservice;

import com.entitykart.productservice.dto.ProductDTO;
import com.entitykart.productservice.entity.ProductEntity;
import com.entitykart.productservice.repository.CategoryRepository;
import com.entitykart.productservice.repository.ProductRepository;
import com.entitykart.productservice.repository.SubCategoryRepository;
import com.entitykart.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SubCategoryRepository subCategoryRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ProductService productService;

    private ProductEntity testProduct;
    private ProductDTO testProductDTO;

    @BeforeEach
    public void setup() {
        testProduct = new ProductEntity();
        testProduct.setProductId(1L);
        testProduct.setProductName("Laptop");
        testProduct.setDescription("Gaming Laptop");
        testProduct.setBrand("BrandX");
        testProduct.setPrice(BigDecimal.valueOf(1000.0));
        testProduct.setMrp(BigDecimal.valueOf(1200.0));
        testProduct.setStockQuantity(10);
        testProduct.setSku("LAP-123");
        testProduct.setMainImageURL("http://image.url");
        testProduct.setCategoryId(5L);
        testProduct.setSubCategoryId(15L);
        testProduct.setSellerId(100L);
        testProduct.setStatus("Available");

        testProductDTO = new ProductDTO();
        testProductDTO.setProductName("Laptop");
        testProductDTO.setPrice(BigDecimal.valueOf(1000.0));
        testProductDTO.setStockQuantity(10);
        testProductDTO.setCategoryId(5L);
        testProductDTO.setSellerId(100L);
    }

    // --- PRODUCT MANAGEMENT TESTS (6 Tests) ---

    @Test
    public void testCreateProduct_Success() {
        when(productRepository.save(any(ProductEntity.class))).thenReturn(testProduct);

        ProductDTO created = productService.createProduct(testProductDTO);

        assertNotNull(created);
        assertEquals("Laptop", created.getProductName());
        verify(productRepository, times(1)).save(any(ProductEntity.class));
        verify(kafkaTemplate, times(1)).send(eq("product-events"), any());
    }

    @Test
    public void testGetProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        ProductDTO fetched = productService.getProduct(1L);

        assertNotNull(fetched);
        assertEquals(1L, fetched.getProductId());
    }

    @Test
    public void testGetProductById_NotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productService.getProduct(99L));
    }

    @Test
    public void testUpdateProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(testProduct);

        ProductDTO updated = productService.updateProduct(1L, testProductDTO);

        assertNotNull(updated);
        assertEquals("Laptop", updated.getProductName());
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    public void testDeleteProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        doNothing().when(productRepository).delete(testProduct);

        assertDoesNotThrow(() -> productService.deleteProduct(1L));
        verify(productRepository, times(1)).delete(testProduct);
    }

    @Test
    public void testDeleteProduct_NotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productService.deleteProduct(99L));
    }

    // --- SEARCH / BATCH RETRIEVAL TESTS (5 Tests) ---

    @Test
    public void testGetProductsByIds_Success() {
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(testProduct));

        List<ProductDTO> list = productService.getProductsByIds(List.of(1L));

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).getProductId());
    }

    @Test
    public void testGetProductsByIds_EmptyList() {
        List<ProductDTO> list = productService.getProductsByIds(Collections.emptyList());
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testGetProductsByIds_NullList() {
        List<ProductDTO> list = productService.getProductsByIds(null);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testGetProductsByCategory() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductEntity> page = new PageImpl<>(List.of(testProduct), pageable, 1);
        when(productRepository.findByCategoryId(5L, pageable)).thenReturn(page);

        Page<ProductDTO> result = productService.getProductsByCategory(5L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    public void testGetProductsBySeller() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductEntity> page = new PageImpl<>(List.of(testProduct), pageable, 1);
        when(productRepository.findBySellerId(100L, pageable)).thenReturn(page);

        Page<ProductDTO> result = productService.getProductsBySeller(100L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    // --- STATS & UTILITY TESTS (4 Tests) ---

    @Test
    public void testGetProductStats() {
        when(productRepository.count()).thenReturn(50L);
        when(categoryRepository.count()).thenReturn(10L);
        when(subCategoryRepository.count()).thenReturn(20L);

        Map<String, Object> stats = productService.getProductStats();

        assertNotNull(stats);
        assertEquals(50L, stats.get("totalProducts"));
        assertEquals(10L, stats.get("totalCategories"));
        assertEquals(20L, stats.get("totalSubCategories"));
    }

    @Test
    public void testGetDiscountPercent() {
        // testProduct has Price=1000 and MRP=1200, so discount should be (1200-1000)/1200 * 100 = 16.67%
        BigDecimal discount = testProduct.getDiscountPercent();
        assertNotNull(discount);
        assertTrue(discount.compareTo(BigDecimal.valueOf(16.0)) > 0);
    }

    @Test
    public void testGetDiscountPercent_ZeroMRP() {
        testProduct.setMrp(BigDecimal.ZERO);
        BigDecimal discount = testProduct.getDiscountPercent();
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    public void testGetProductsFiltered() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductEntity> page = new PageImpl<>(List.of(testProduct), pageable, 1);
        when(productRepository.filterProducts(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        Page<ProductDTO> result = productService.getProductsFiltered(5L, null, "Laptop", null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
