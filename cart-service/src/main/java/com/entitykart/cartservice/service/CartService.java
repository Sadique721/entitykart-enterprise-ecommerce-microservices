package com.entitykart.cartservice.service;

import com.entitykart.cartservice.dto.CartItemDTO;
import com.entitykart.cartservice.dto.CheckoutRequest;
import com.entitykart.cartservice.entity.CartItemEntity;
import com.entitykart.cartservice.repository.CartRepository;
import com.entitykart.cartservice.client.ProductServiceClient;
import com.entitykart.cartservice.client.OrderServiceClient;
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
    private final ProductServiceClient productServiceClient;
    private final OrderServiceClient orderServiceClient;

    @Transactional
    public void addToCart(Long customerId, Long productId, Integer quantity, Double price) {
        validateQuantity(quantity);
        if (quantity > 100) {
            throw new RuntimeException("Cannot add more than 100 units of a product to cart at once");
        }

        // Validate product existence and status via product-service, and fetch actual catalog price
        Double actualPrice;
        try {
            ProductServiceClient.ProductInfo product = productServiceClient.getProduct(productId);
            if (product == null) {
                throw new RuntimeException("Product not found");
            }
            if ("INACTIVE".equalsIgnoreCase(product.getStatus())) {
                throw new RuntimeException("Product is currently unavailable");
            }
            if (product.getStockQuantity() != null && product.getStockQuantity() < quantity) {
                throw new RuntimeException("Insufficient stock. Only " + product.getStockQuantity() + " items available.");
            }
            if (product.getPrice() == null) {
                throw new RuntimeException("Product price is not configured");
            }
            actualPrice = product.getPrice().doubleValue();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Could not validate product info from product-service: {}", e.getMessage());
            throw new RuntimeException("Product validation failed: " + e.getMessage());
        }

        validatePrice(actualPrice);

        CartItemEntity existing = cartRepository.findByCustomerIdAndProductId(customerId, productId).orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            cartRepository.save(existing);
        } else {
            CartItemEntity item = new CartItemEntity();
            item.setCustomerId(customerId);
            item.setProductId(productId);
            item.setQuantity(quantity);
            item.setPrice(actualPrice);
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
            if (quantity > 100) {
                throw new RuntimeException("Cannot exceed 100 units of a product in cart");
            }
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
        if (items.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<Long> productIds = items.stream()
                .map(CartItemEntity::getProductId)
                .distinct()
                .collect(Collectors.toList());

        java.util.Map<Long, ProductServiceClient.ProductInfo> productMap = new java.util.HashMap<>();
        try {
            List<ProductServiceClient.ProductInfo> productInfos = productServiceClient.getProductsBatch(productIds);
            if (productInfos != null) {
                for (ProductServiceClient.ProductInfo info : productInfos) {
                    if (info != null && info.getProductId() != null) {
                        productMap.put(info.getProductId(), info);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not load product details in batch: {}", e.getMessage());
        }

        return items.stream()
                .map(item -> convertToDTO(item, productMap))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Double getCartTotal(Long customerId) {
        Double total = cartRepository.getCartTotal(customerId);
        return total != null ? total : 0.0;
    }

    @Transactional
    public OrderServiceClient.OrderResponse checkout(CheckoutRequest request) {
        Long customerId = request.getCustomerId();
        List<CartItemDTO> items = getCartItems(customerId);
        if (items.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Double total = getCartTotal(customerId);
        
        // Convert to shared DTO model
        List<com.entitykart.shared.dto.CartItemDTO> sharedItems = items.stream()
                .map(item -> new com.entitykart.shared.dto.CartItemDTO(item.getProductId(), item.getQuantity(), item.getPrice()))
                .collect(Collectors.toList());

        com.entitykart.shared.dto.CartCheckoutEvent event = new com.entitykart.shared.dto.CartCheckoutEvent(
                customerId,
                request.getAddressId(),
                sharedItems,
                total,
                request.getPaymentMode()
        );

        // Perform synchronous order creation
        OrderServiceClient.OrderResponse order = orderServiceClient.createOrder(event);
        clearCart(customerId);

        log.info("Synchronous order creation completed for customer {} -> orderId: {}", customerId, order.getOrderId());
        return order;
    }

    private CartItemDTO convertToDTO(CartItemEntity entity, java.util.Map<Long, ProductServiceClient.ProductInfo> productMap) {
        CartItemDTO dto = new CartItemDTO();
        dto.setCartItemId(entity.getCartItemId());
        dto.setProductId(entity.getProductId());
        dto.setQuantity(entity.getQuantity());
        dto.setPrice(entity.getPrice());
        dto.setSubtotal(entity.getQuantity() * entity.getPrice());

        ProductServiceClient.ProductInfo info = productMap.get(entity.getProductId());
        if (info != null) {
            dto.setProductName(info.getProductName());
            dto.setMainImageURL(info.getMainImageURL());
        } else {
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

    /**
     * MED-5: Server-side coupon validation.
     * Add DB-backed coupon table in v2.1 for dynamic coupons.
     */
    public com.entitykart.cartservice.dto.CouponValidationResponse validateCoupon(String code, Double cartTotal) {
        if (code == null || code.isBlank()) {
            return new com.entitykart.cartservice.dto.CouponValidationResponse(
                    false, code, null, null, null, "Invalid coupon code");
        }

        String upper = code.trim().toUpperCase();

        return switch (upper) {
            case "SAVE10" -> new com.entitykart.cartservice.dto.CouponValidationResponse(
                    true, upper, "PERCENT", 10.0, 500.0, "10% off (max ₹500)");
            case "FLAT100" -> {
                if (cartTotal < 500) {
                    yield new com.entitykart.cartservice.dto.CouponValidationResponse(
                            false, upper, "FIXED", 100.0, null, "Minimum order ₹500 required");
                }
                yield new com.entitykart.cartservice.dto.CouponValidationResponse(
                        true, upper, "FIXED", 100.0, null, "Flat ₹100 off");
            }
            case "ENTITYKART20" -> {
                if (cartTotal < 1000) {
                    yield new com.entitykart.cartservice.dto.CouponValidationResponse(
                            false, upper, "PERCENT", 20.0, 1000.0, "Minimum order ₹1000 required");
                }
                yield new com.entitykart.cartservice.dto.CouponValidationResponse(
                        true, upper, "PERCENT", 20.0, 1000.0, "20% off (max ₹1000)");
            }
            default -> new com.entitykart.cartservice.dto.CouponValidationResponse(
                    false, code, null, null, null, "Coupon not found or expired");
        };
    }
}
