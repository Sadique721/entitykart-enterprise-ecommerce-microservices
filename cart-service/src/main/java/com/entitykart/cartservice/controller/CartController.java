package com.entitykart.cartservice.controller;

import com.entitykart.cartservice.dto.CartItemDTO;
import com.entitykart.cartservice.dto.CheckoutRequest;
import com.entitykart.cartservice.dto.CouponValidationResponse;
import com.entitykart.cartservice.service.CartService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestBody;
import com.entitykart.cartservice.dto.CheckoutRequest;
import com.entitykart.cartservice.client.OrderServiceClient;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Issue 3 fix: verify that the caller owns the customerId they are accessing.
     * The gateway injects X-Customer-Id and X-User-Role from the validated JWT.
     * ADMINs are exempt so support/admin tooling continues to work unchanged.
     */
    private void verifyOwnership(Long loggedInId, String role, Long requestedId) {
        if (loggedInId != null && !requestedId.equals(loggedInId) && !"ADMIN".equalsIgnoreCase(role)) {
            throw new RuntimeException("Unauthorized: cannot access another customer's cart");
        }
    }

    @PostMapping("/add")
    public void addToCart(
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
            @RequestParam Long customerId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        verifyOwnership(loggedInCustomerId, loggedInUserRole, customerId);
        cartService.addToCart(customerId, productId, quantity, null);
    }

    @PutMapping("/update")
    public void updateQuantity(
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
            @RequestParam Long customerId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        verifyOwnership(loggedInCustomerId, loggedInUserRole, customerId);
        cartService.updateQuantity(customerId, productId, quantity);
    }

    @DeleteMapping("/remove")
    public void removeItem(
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
            @RequestParam Long customerId,
            @RequestParam Long productId) {
        verifyOwnership(loggedInCustomerId, loggedInUserRole, customerId);
        cartService.removeItem(customerId, productId);
    }

    @DeleteMapping("/clear")
    public void clearCart(
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
            @RequestParam Long customerId) {
        verifyOwnership(loggedInCustomerId, loggedInUserRole, customerId);
        cartService.clearCart(customerId);
    }

    @GetMapping
    public List<CartItemDTO> getCart(
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
            @RequestParam Long customerId) {
        verifyOwnership(loggedInCustomerId, loggedInUserRole, customerId);
        return cartService.getCartItems(customerId);
    }

    @GetMapping("/total")
    public Double getCartTotal(
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
            @RequestParam Long customerId) {
        verifyOwnership(loggedInCustomerId, loggedInUserRole, customerId);
        return cartService.getCartTotal(customerId);
    }

    @PostMapping("/checkout")
    public OrderServiceClient.OrderResponse checkout(
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
            @RequestBody CheckoutRequest request) {
        verifyOwnership(loggedInCustomerId, loggedInUserRole, request.getCustomerId());
        return cartService.checkout(request);
    }

    /**
     * MED-5: Server-side coupon/promo code validation.
     * Prevents clients from forging discount amounts on checkout.
     */
    @PostMapping("/validate-coupon")
    public CouponValidationResponse validateCoupon(
            @RequestParam String code,
            @RequestParam Double cartTotal) {
        return cartService.validateCoupon(code, cartTotal);
    }
}
