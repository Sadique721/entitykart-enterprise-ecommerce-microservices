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

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public OrderDTO getOrder(@PathVariable Long orderId) {
        return orderService.getOrder(orderId);
    }

    @GetMapping("/customer/{customerId}")
    public List<OrderDTO> getCustomerOrders(@PathVariable Long customerId) {
        return orderService.getOrdersByCustomer(customerId);
    }

    @GetMapping("/all")
    public List<OrderDTO> getAllOrders() {
        return orderService.getAllOrders();
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
