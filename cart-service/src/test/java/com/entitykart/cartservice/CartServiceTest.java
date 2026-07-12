package com.entitykart.cartservice;

import com.entitykart.cartservice.client.OrderServiceClient;
import com.entitykart.cartservice.client.ProductServiceClient;
import com.entitykart.cartservice.dto.CartItemDTO;
import com.entitykart.cartservice.dto.CheckoutRequest;
import com.entitykart.cartservice.entity.CartItemEntity;
import com.entitykart.cartservice.repository.CartRepository;
import com.entitykart.cartservice.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private CartService cartService;

    private ProductServiceClient.ProductInfo testProduct;
    private CartItemEntity testItem;

    @BeforeEach
    public void setup() {
        testProduct = new ProductServiceClient.ProductInfo();
        testProduct.setProductId(101L);
        testProduct.setProductName("Test Product");
        testProduct.setPrice(BigDecimal.valueOf(250.0));
        testProduct.setStockQuantity(50);
        testProduct.setStatus("ACTIVE");
        testProduct.setMainImageURL("http://image.url");

        testItem = new CartItemEntity();
        testItem.setCartItemId(1L);
        testItem.setCustomerId(1L);
        testItem.setProductId(101L);
        testItem.setQuantity(2);
        testItem.setPrice(250.0);
    }

    // --- ADD TO CART TESTS (7 Tests) ---

    @Test
    public void testAddToCart_Success_NewItem() {
        when(productServiceClient.getProduct(101L)).thenReturn(testProduct);
        when(cartRepository.findByCustomerIdAndProductId(1L, 101L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(CartItemEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        assertDoesNotThrow(() -> cartService.addToCart(1L, 101L, 2, 250.0));
        verify(cartRepository, times(1)).save(any(CartItemEntity.class));
    }

    @Test
    public void testAddToCart_Success_ExistingItem() {
        when(productServiceClient.getProduct(101L)).thenReturn(testProduct);
        when(cartRepository.findByCustomerIdAndProductId(1L, 101L)).thenReturn(Optional.of(testItem));
        when(cartRepository.save(any(CartItemEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        assertDoesNotThrow(() -> cartService.addToCart(1L, 101L, 3, 250.0));
        assertEquals(5, testItem.getQuantity());
        verify(cartRepository, times(1)).save(testItem);
    }

    @Test
    public void testAddToCart_InvalidQuantity() {
        assertThrows(RuntimeException.class, () -> cartService.addToCart(1L, 101L, 0, 250.0));
        assertThrows(RuntimeException.class, () -> cartService.addToCart(1L, 101L, -5, 250.0));
    }

    @Test
    public void testAddToCart_QuantityExceedsLimit() {
        assertThrows(RuntimeException.class, () -> cartService.addToCart(1L, 101L, 101, 250.0));
    }

    @Test
    public void testAddToCart_ProductNotFound() {
        when(productServiceClient.getProduct(101L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> cartService.addToCart(1L, 101L, 2, 250.0));
    }

    @Test
    public void testAddToCart_ProductInactive() {
        testProduct.setStatus("INACTIVE");
        when(productServiceClient.getProduct(101L)).thenReturn(testProduct);
        assertThrows(RuntimeException.class, () -> cartService.addToCart(1L, 101L, 2, 250.0));
    }

    @Test
    public void testAddToCart_InsufficientStock() {
        testProduct.setStockQuantity(1);
        when(productServiceClient.getProduct(101L)).thenReturn(testProduct);
        assertThrows(RuntimeException.class, () -> cartService.addToCart(1L, 101L, 2, 250.0));
    }

    // --- UPDATE QUANTITY TESTS (4 Tests) ---

    @Test
    public void testUpdateQuantity_Increase() {
        when(cartRepository.findByCustomerIdAndProductId(1L, 101L)).thenReturn(Optional.of(testItem));
        when(cartRepository.save(any(CartItemEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        assertDoesNotThrow(() -> cartService.updateQuantity(1L, 101L, 5));
        assertEquals(5, testItem.getQuantity());
        verify(cartRepository, times(1)).save(testItem);
    }

    @Test
    public void testUpdateQuantity_ExceedsLimit() {
        when(cartRepository.findByCustomerIdAndProductId(1L, 101L)).thenReturn(Optional.of(testItem));
        assertThrows(RuntimeException.class, () -> cartService.updateQuantity(1L, 101L, 105));
    }

    @Test
    public void testUpdateQuantity_ZeroRemovesItem() {
        when(cartRepository.findByCustomerIdAndProductId(1L, 101L)).thenReturn(Optional.of(testItem));
        doNothing().when(cartRepository).delete(testItem);

        assertDoesNotThrow(() -> cartService.updateQuantity(1L, 101L, 0));
        verify(cartRepository, times(1)).delete(testItem);
    }

    @Test
    public void testUpdateQuantity_ItemNotInCart() {
        when(cartRepository.findByCustomerIdAndProductId(1L, 101L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> cartService.updateQuantity(1L, 101L, 5));
    }

    // --- REMOVE & CLEAR TESTS (2 Tests) ---

    @Test
    public void testRemoveItem() {
        doNothing().when(cartRepository).deleteByCustomerIdAndProductId(1L, 101L);

        assertDoesNotThrow(() -> cartService.removeItem(1L, 101L));
        verify(cartRepository, times(1)).deleteByCustomerIdAndProductId(1L, 101L);
    }

    @Test
    public void testClearCart() {
        doNothing().when(cartRepository).deleteByCustomerId(1L);

        assertDoesNotThrow(() -> cartService.clearCart(1L));
        verify(cartRepository, times(1)).deleteByCustomerId(1L);
    }

    // --- RETRIEVAL & CHECKOUT TESTS (2 Tests) ---

    @Test
    public void testGetCartItems_BatchOptimization() {
        when(cartRepository.findByCustomerId(1L)).thenReturn(List.of(testItem));
        when(productServiceClient.getProductsBatch(List.of(101L))).thenReturn(List.of(testProduct));

        List<CartItemDTO> dtos = cartService.getCartItems(1L);

        assertNotNull(dtos);
        assertEquals(1, dtos.size());
        assertEquals("Test Product", dtos.get(0).getProductName());
        assertEquals("http://image.url", dtos.get(0).getMainImageURL());
    }

    @Test
    public void testCheckout_EmptyCart_ThrowsException() {
        when(cartRepository.findByCustomerId(1L)).thenReturn(Collections.emptyList());
        CheckoutRequest req = new CheckoutRequest();
        req.setCustomerId(1L);

        assertThrows(RuntimeException.class, () -> cartService.checkout(req));
    }
}
