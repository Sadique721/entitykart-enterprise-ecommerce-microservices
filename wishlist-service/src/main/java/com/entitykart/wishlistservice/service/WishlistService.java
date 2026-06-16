package com.entitykart.wishlistservice.service;

import com.entitykart.wishlistservice.client.ProductServiceClient;
import com.entitykart.wishlistservice.dto.WishlistItemDTO;
import com.entitykart.wishlistservice.entity.WishlistItemEntity;
import com.entitykart.wishlistservice.repository.WishlistRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductServiceClient productClient;

    @Transactional
    public void addToWishlist(Long customerId, Long productId) {
        if (wishlistRepository.existsByCustomerIdAndProductId(customerId, productId)) {
            throw new RuntimeException("Product already in wishlist");
        }

        WishlistItemEntity item = new WishlistItemEntity();
        item.setCustomerId(customerId);
        item.setProductId(productId);
        wishlistRepository.save(item);

        log.info("Added product {} to wishlist of customer {}", productId, customerId);
    }

    @Transactional
    public void removeFromWishlist(Long customerId, Long productId) {
        wishlistRepository.deleteByCustomerIdAndProductId(customerId, productId);
        log.info("Removed product {} from wishlist of customer {}", productId, customerId);
    }

    @Transactional
    public void clearWishlist(Long customerId) {
        wishlistRepository.deleteByCustomerId(customerId);
        log.info("Cleared wishlist for customer {}", customerId);
    }

    @Transactional(readOnly = true)
    public List<WishlistItemDTO> getWishlist(Long customerId) {
        List<WishlistItemEntity> items = wishlistRepository.findByCustomerId(customerId);
        return items.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<WishlistItemDTO> getWishlistPaginated(Long customerId, Pageable pageable) {
        Page<WishlistItemEntity> page = wishlistRepository.findByCustomerIdOrderByAddedAtDesc(customerId, pageable);
        return page.map(this::convertToDTO);
    }

    private WishlistItemDTO convertToDTO(WishlistItemEntity entity) {
        WishlistItemDTO dto = new WishlistItemDTO();
        dto.setWishlistId(entity.getWishlistId());
        dto.setProductId(entity.getProductId());
        dto.setAddedAt(entity.getAddedAt());

        try {
            ProductServiceClient.ProductInfo product = productClient.getProduct(entity.getProductId());
            dto.setProductName(product.getProductName());
            dto.setProductImage(product.getMainImageURL());
            if (product.getPrice() != null) {
                dto.setPrice(product.getPrice().doubleValue());
            }
        } catch (Exception exception) {
            log.error("Failed to fetch product details for id: {}", entity.getProductId(), exception);
            dto.setProductName("Unknown Product");
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public List<WishlistItemDTO> getAllWishlistItems() {
        return wishlistRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }
}
