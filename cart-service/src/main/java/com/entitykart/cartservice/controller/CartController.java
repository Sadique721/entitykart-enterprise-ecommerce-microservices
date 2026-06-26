package com.entitykart.cartservice.controller;

import com.entitykart.cartservice.dto.CartItemDTO;
import com.entitykart.cartservice.service.CartService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public void addToCart(
            @RequestParam Long customerId,
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            @RequestParam Double price) {
        cartService.addToCart(customerId, productId, quantity, price);
    }

    @PutMapping("/update")
    public void updateQuantity(
            @RequestParam Long customerId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        cartService.updateQuantity(customerId, productId, quantity);
    }

    @DeleteMapping("/remove")
    public void removeItem(@RequestParam Long customerId, @RequestParam Long productId) {
        cartService.removeItem(customerId, productId);
    }

    @DeleteMapping("/clear")
    public void clearCart(@RequestParam Long customerId) {
        cartService.clearCart(customerId);
    }

    @GetMapping
    public List<CartItemDTO> getCart(@RequestParam Long customerId) {
        return cartService.getCartItems(customerId);
    }

    @GetMapping("/total")
    public Double getCartTotal(@RequestParam Long customerId) {
        return cartService.getCartTotal(customerId);
    }

    @PostMapping("/checkout")
    public void checkout(
            @RequestParam Long customerId,
            @RequestParam Long addressId,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) String cardNumber,
            @RequestParam(required = false) String expiry,
            @RequestParam(required = false) String cvv,
            @RequestParam(required = false) String upiId) {
        cartService.checkout(customerId, addressId, paymentMode, cardNumber, expiry, cvv, upiId);
    }
}
