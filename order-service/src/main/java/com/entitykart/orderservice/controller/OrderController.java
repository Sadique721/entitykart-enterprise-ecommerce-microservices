package com.entitykart.orderservice.controller;

import com.entitykart.orderservice.dto.OrderDTO;
import com.entitykart.orderservice.service.OrderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public OrderDTO createOrder(@RequestBody com.entitykart.shared.dto.CartCheckoutEvent event) {
        return orderService.createOrder(event);
    }

    @GetMapping("/{orderId}")
    public OrderDTO getOrder(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role", required = false) String loggedInUserRole) {
        OrderDTO order = orderService.getOrder(orderId);
        if (loggedInCustomerId != null && !order.getCustomerId().equals(loggedInCustomerId) && !"ADMIN".equalsIgnoreCase(loggedInUserRole)) {
            throw new RuntimeException("Unauthorized to view this order");
        }
        return order;
    }

    @GetMapping("/customer/{customerId}")
    public Page<OrderDTO> getCustomerOrders(
            @PathVariable Long customerId,
            Pageable pageable,
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role", required = false) String loggedInUserRole) {
        if (loggedInCustomerId != null && !customerId.equals(loggedInCustomerId) && !"ADMIN".equalsIgnoreCase(loggedInUserRole)) {
            throw new RuntimeException("Unauthorized to view these orders");
        }
        return orderService.getOrdersByCustomer(customerId, pageable);
    }

    @GetMapping("/all")
    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        return orderService.getAllOrders(pageable);
    }

    /** Used by admin frontend to update order status (PLACED → CONFIRMED → SHIPPED → DELIVERED → CANCELLED) */
    @PutMapping("/{orderId}/status")
    public void updateStatus(@PathVariable Long orderId, @RequestParam String status) {
        orderService.updateOrderStatus(orderId, status);
    }

    /** Used by return-service FeignClient (PATCH) */
    @PatchMapping("/{orderId}/status")
    public void patchStatus(@PathVariable Long orderId, @RequestParam String status) {
        orderService.updateOrderStatus(orderId, status);
    }

    /** Used by payment-service FeignClient to update payment status (PAID / UNPAID) */
    @PutMapping("/{orderId}/payment-status")
    public void updatePaymentStatus(@PathVariable Long orderId, @RequestParam String status) {
        orderService.updatePaymentStatus(orderId, status);
    }
}
