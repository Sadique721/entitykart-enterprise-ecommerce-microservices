package com.entitykart.wishlistservice.controller;

import com.entitykart.wishlistservice.dto.WishlistItemDTO;
import com.entitykart.wishlistservice.service.WishlistService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/add")
    public void addToWishlist(@RequestParam Long customerId, @RequestParam Long productId) {
        wishlistService.addToWishlist(customerId, productId);
    }

    @DeleteMapping("/remove")
    public void removeFromWishlist(@RequestParam Long customerId, @RequestParam Long productId) {
        wishlistService.removeFromWishlist(customerId, productId);
    }

    @DeleteMapping("/clear")
    public void clearWishlist(@RequestParam Long customerId) {
        wishlistService.clearWishlist(customerId);
    }

    @GetMapping
    public List<WishlistItemDTO> getWishlist(@RequestParam Long customerId) {
        return wishlistService.getWishlist(customerId);
    }

    @GetMapping("/paginated")
    public Page<WishlistItemDTO> getWishlistPaginated(@RequestParam Long customerId, Pageable pageable) {
        return wishlistService.getWishlistPaginated(customerId, pageable);
    }

    @GetMapping("/all")
    public List<WishlistItemDTO> getAllWishlistItems() {
        return wishlistService.getAllWishlistItems();
    }
}
