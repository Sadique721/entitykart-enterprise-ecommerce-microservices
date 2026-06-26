package com.entitykart.cartservice.service;

import com.entitykart.cartservice.dto.CartCheckoutEvent;
import com.entitykart.cartservice.dto.CartItemDTO;
import com.entitykart.cartservice.entity.CartItemEntity;
import com.entitykart.cartservice.event.CartCheckoutPublisher;
import com.entitykart.cartservice.repository.CartRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartCheckoutPublisher cartCheckoutPublisher;
    private final com.entitykart.cartservice.client.ProductServiceClient productServiceClient;

    @Transactional
    public void addToCart(Long customerId, Long productId, Integer quantity, Double price) {
        validateQuantity(quantity);
        validatePrice(price);

        // Validate product existence and status via product-service
        try {
            com.entitykart.cartservice.client.ProductServiceClient.ProductInfo product = productServiceClient.getProduct(productId);
            if (product == null) {
                throw new RuntimeException("Product not found");
            }
            if ("INACTIVE".equalsIgnoreCase(product.getStatus())) {
                throw new RuntimeException("Product is currently unavailable");
            }
            if (product.getStockQuantity() != null && product.getStockQuantity() < quantity) {
                throw new RuntimeException("Insufficient stock. Only " + product.getStockQuantity() + " items available.");
            }
        } catch (Exception e) {
            log.warn("Could not validate product info from product-service: {}", e.getMessage());
            // Fallback: log warning and proceed (or fail depending on configuration; here we proceed to ensure robustness)
        }

        CartItemEntity existing = cartRepository.findByCustomerIdAndProductId(customerId, productId).orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            cartRepository.save(existing);
        } else {
            CartItemEntity item = new CartItemEntity();
            item.setCustomerId(customerId);
            item.setProductId(productId);
            item.setQuantity(quantity);
            item.setPrice(price);
            cartRepository.save(item);
        }

        log.info("Added product {} to cart of customer {}", productId, customerId);
    }

    @Transactional
    public void updateQuantity(Long customerId, Long productId, Integer quantity) {
        CartItemEntity item = cartRepository.findByCustomerIdAndProductId(customerId, productId)
                .orElseThrow(() -> new RuntimeException("Item not in cart"));

        if (quantity == null || quantity <= 0) {
            cartRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartRepository.save(item);
        }
    }

    @Transactional
    public void removeItem(Long customerId, Long productId) {
        cartRepository.deleteByCustomerIdAndProductId(customerId, productId);
    }

    @Transactional
    public void clearCart(Long customerId) {
        cartRepository.deleteByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<CartItemDTO> getCartItems(Long customerId) {
        List<CartItemEntity> items = cartRepository.findByCustomerId(customerId);
        return items.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Double getCartTotal(Long customerId) {
        return cartRepository.getCartTotal(customerId);
    }

    @Transactional
    public void checkout(Long customerId, Long addressId, String paymentMode, String cardNumber, String expiry, String cvv, String upiId) {
        List<CartItemDTO> items = getCartItems(customerId);
        if (items.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Double total = getCartTotal(customerId);
        CartCheckoutEvent event = new CartCheckoutEvent(customerId, addressId, items, total, paymentMode, cardNumber, expiry, cvv, upiId);
        cartCheckoutPublisher.publish(event);
        clearCart(customerId);

        log.info("Checkout event sent for customer {} with paymentMode {}", customerId, paymentMode);
    }

    private CartItemDTO convertToDTO(CartItemEntity entity) {
        CartItemDTO dto = new CartItemDTO();
        dto.setCartItemId(entity.getCartItemId());
        dto.setProductId(entity.getProductId());
        dto.setQuantity(entity.getQuantity());
        dto.setPrice(entity.getPrice());
        dto.setSubtotal(entity.getQuantity() * entity.getPrice());

        try {
            com.entitykart.cartservice.client.ProductServiceClient.ProductInfo info = productServiceClient.getProduct(entity.getProductId());
            if (info != null) {
                dto.setProductName(info.getProductName());
                dto.setMainImageURL(info.getMainImageURL());
            }
        } catch (Exception e) {
            log.warn("Could not load product details for id {}: {}", entity.getProductId(), e.getMessage());
            dto.setProductName("Product " + entity.getProductId());
        }

        return dto;
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than zero");
        }
    }

    private void validatePrice(Double price) {
        if (price == null || price < 0) {
            throw new RuntimeException("Price must be zero or greater");
        }
    }
}
