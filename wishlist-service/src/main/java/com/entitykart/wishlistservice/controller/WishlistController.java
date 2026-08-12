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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    /**
     * Issue 3 fix: verify that the caller owns the customerId they are accessing.
     * The gateway injects X-Customer-Id and X-User-Role from the validated JWT.
     * ADMINs are exempt so admin tooling can view/clear any customer's wishlist.
     */
    private void verifyOwnership(Long loggedInId, String role, Long requestedId) {
        if (loggedInId != null && !requestedId.equals(loggedInId) && !"ADMIN".equalsIgnoreCase(role)) {
            throw new RuntimeException("Unauthorized: cannot access another customer's wishlist");
        }
    }

    @PostMapping("/add")
    public void addToWishlist(
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
            @RequestParam Long customerId,
            @RequestParam Long productId) {
        verifyOwnership(loggedInCustomerId, loggedInUserRole, customerId);
        wishlistService.addToWishlist(customerId, productId);
    }

    @DeleteMapping("/remove")
    public void removeFromWishlist(
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
            @RequestParam Long customerId,
            @RequestParam Long productId) {
        verifyOwnership(loggedInCustomerId, loggedInUserRole, customerId);
        wishlistService.removeFromWishlist(customerId, productId);
    }

    @DeleteMapping("/clear")
    public void clearWishlist(
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
            @RequestParam Long customerId) {
        verifyOwnership(loggedInCustomerId, loggedInUserRole, customerId);
        wishlistService.clearWishlist(customerId);
    }

    @GetMapping
    public List<WishlistItemDTO> getWishlist(
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
            @RequestParam Long customerId) {
        verifyOwnership(loggedInCustomerId, loggedInUserRole, customerId);
        return wishlistService.getWishlist(customerId);
    }

    @GetMapping("/paginated")
    public Page<WishlistItemDTO> getWishlistPaginated(
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
            @RequestParam Long customerId,
            Pageable pageable) {
        verifyOwnership(loggedInCustomerId, loggedInUserRole, customerId);
        return wishlistService.getWishlistPaginated(customerId, pageable);
    }

    /** Admin-only: returns all wishlist items across all customers. */
    @GetMapping("/all")
    public List<WishlistItemDTO> getAllWishlistItems(
            @RequestHeader(value = "X-User-Role", required = false) String loggedInUserRole) {
        if (!"ADMIN".equalsIgnoreCase(loggedInUserRole)) {
            throw new RuntimeException("Access Denied: Admin role required");
        }
        return wishlistService.getAllWishlistItems();
    }
}
