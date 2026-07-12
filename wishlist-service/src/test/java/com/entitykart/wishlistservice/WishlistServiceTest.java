package com.entitykart.wishlistservice;

import com.entitykart.wishlistservice.client.ProductServiceClient;
import com.entitykart.wishlistservice.dto.WishlistItemDTO;
import com.entitykart.wishlistservice.entity.WishlistItemEntity;
import com.entitykart.wishlistservice.repository.WishlistRepository;
import com.entitykart.wishlistservice.service.WishlistService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private ProductServiceClient productClient;

    @InjectMocks
    private WishlistService wishlistService;

    private WishlistItemEntity testItem;
    private ProductServiceClient.ProductInfo testProduct;

    @BeforeEach
    public void setup() {
        testItem = new WishlistItemEntity();
        testItem.setWishlistId(1L);
        testItem.setCustomerId(10L);
        testItem.setProductId(101L);
        testItem.setAddedAt(LocalDateTime.now());

        testProduct = new ProductServiceClient.ProductInfo();
        testProduct.setProductId(101L);
        testProduct.setProductName("Cool Laptop");
        testProduct.setMainImageURL("http://laptop.img");
        testProduct.setPrice(BigDecimal.valueOf(999.0));
    }

    // --- ADD TESTS (5 Tests) ---

    @Test
    public void testAddToWishlist_Success() {
        when(wishlistRepository.existsByCustomerIdAndProductId(10L, 101L)).thenReturn(false);
        when(wishlistRepository.save(any(WishlistItemEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        assertDoesNotThrow(() -> wishlistService.addToWishlist(10L, 101L));
        verify(wishlistRepository, times(1)).save(any(WishlistItemEntity.class));
    }

    @Test
    public void testAddToWishlist_Duplicate_ThrowsException() {
        when(wishlistRepository.existsByCustomerIdAndProductId(10L, 101L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> wishlistService.addToWishlist(10L, 101L));
    }

    @Test
    public void testAddToWishlist_NullCustomerId() {
        assertThrows(RuntimeException.class, () -> wishlistService.addToWishlist(null, 101L));
    }

    @Test
    public void testAddToWishlist_NullProductId() {
        assertThrows(RuntimeException.class, () -> wishlistService.addToWishlist(10L, null));
    }

    @Test
    public void testAddToWishlist_CheckLogTriggered() {
        when(wishlistRepository.existsByCustomerIdAndProductId(10L, 101L)).thenReturn(false);
        wishlistService.addToWishlist(10L, 101L);
        verify(wishlistRepository, times(1)).save(any(WishlistItemEntity.class));
    }

    // --- REMOVE TESTS (5 Tests) ---

    @Test
    public void testRemoveFromWishlist_Success() {
        doNothing().when(wishlistRepository).deleteByCustomerIdAndProductId(10L, 101L);

        assertDoesNotThrow(() -> wishlistService.removeFromWishlist(10L, 101L));
        verify(wishlistRepository, times(1)).deleteByCustomerIdAndProductId(10L, 101L);
    }

    @Test
    public void testRemoveFromWishlist_NullCustomerId() {
        assertDoesNotThrow(() -> wishlistService.removeFromWishlist(null, 101L));
    }

    @Test
    public void testRemoveFromWishlist_NullProductId() {
        assertDoesNotThrow(() -> wishlistService.removeFromWishlist(10L, null));
    }

    @Test
    public void testClearWishlist_Success() {
        doNothing().when(wishlistRepository).deleteByCustomerId(10L);

        assertDoesNotThrow(() -> wishlistService.clearWishlist(10L));
        verify(wishlistRepository, times(1)).deleteByCustomerId(10L);
    }

    @Test
    public void testClearWishlist_NullCustomerId() {
        assertDoesNotThrow(() -> wishlistService.clearWishlist(null));
    }

    // --- QUERY TESTS (5 Tests) ---

    @Test
    public void testGetWishlist_Success() {
        when(wishlistRepository.findByCustomerId(10L)).thenReturn(List.of(testItem));
        when(productClient.getProduct(101L)).thenReturn(testProduct);

        List<WishlistItemDTO> list = wishlistService.getWishlist(10L);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Cool Laptop", list.get(0).getProductName());
        assertEquals(999.0, list.get(0).getPrice());
    }

    @Test
    public void testGetWishlist_ProductServiceOutage_HandlesGracefully() {
        when(wishlistRepository.findByCustomerId(10L)).thenReturn(List.of(testItem));
        when(productClient.getProduct(101L)).thenThrow(new RuntimeException("Product Service Down"));

        List<WishlistItemDTO> list = wishlistService.getWishlist(10L);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Unknown Product", list.get(0).getProductName());
    }

    @Test
    public void testGetWishlistPaginated_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<WishlistItemEntity> page = new PageImpl<>(List.of(testItem), pageable, 1);
        when(wishlistRepository.findByCustomerIdOrderByAddedAtDesc(10L, pageable)).thenReturn(page);
        when(productClient.getProduct(101L)).thenReturn(testProduct);

        Page<WishlistItemDTO> result = wishlistService.getWishlistPaginated(10L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Cool Laptop", result.getContent().get(0).getProductName());
    }

    @Test
    public void testGetAllWishlistItems_Success() {
        when(wishlistRepository.findAll()).thenReturn(List.of(testItem));
        when(productClient.getProduct(101L)).thenReturn(testProduct);

        List<WishlistItemDTO> list = wishlistService.getAllWishlistItems();

        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    public void testConvertToDTO_NullPrice_HandlesGracefully() {
        testProduct.setPrice(null);
        when(wishlistRepository.findByCustomerId(10L)).thenReturn(List.of(testItem));
        when(productClient.getProduct(101L)).thenReturn(testProduct);

        List<WishlistItemDTO> list = wishlistService.getWishlist(10L);

        assertNotNull(list);
        assertNull(list.get(0).getPrice());
    }
}
