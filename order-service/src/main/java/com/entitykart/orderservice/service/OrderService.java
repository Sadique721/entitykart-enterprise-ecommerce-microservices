package com.entitykart.orderservice.service;

import com.entitykart.orderservice.dto.OrderDTO;
import com.entitykart.orderservice.dto.OrderItemDTO;
import com.entitykart.orderservice.entity.OrderEntity;
import com.entitykart.orderservice.entity.OrderItemEntity;
import com.entitykart.orderservice.repository.OrderItemRepository;
import com.entitykart.orderservice.repository.OrderRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public OrderDTO getOrder(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return convertToDTO(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (isPaymentStatus(status)) {
            OrderEntity.PaymentStatus paymentStatus = OrderEntity.PaymentStatus.valueOf(status);
            order.setPaymentStatus(paymentStatus);
            if (paymentStatus == OrderEntity.PaymentStatus.PAID
                    && order.getOrderStatus() == OrderEntity.OrderStatus.PENDING_PAYMENT) {
                order.setOrderStatus(OrderEntity.OrderStatus.PLACED);
            }
        } else {
            order.setOrderStatus(OrderEntity.OrderStatus.valueOf(status));
        }

        orderRepository.save(order);
    }

    private boolean isPaymentStatus(String status) {
        try {
            OrderEntity.PaymentStatus.valueOf(status);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private OrderDTO convertToDTO(OrderEntity entity) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(entity.getOrderId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setAddressId(entity.getAddressId());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setOrderStatus(entity.getOrderStatus().name());
        dto.setPaymentStatus(entity.getPaymentStatus().name());
        dto.setOrderDate(entity.getOrderDate());

        List<OrderItemEntity> items = orderItemRepository.findByOrderId(entity.getOrderId());
        dto.setItems(items.stream().map(this::convertItemToDTO).collect(Collectors.toList()));
        return dto;
    }

    private OrderItemDTO convertItemToDTO(OrderItemEntity entity) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setProductId(entity.getProductId());
        dto.setQuantity(entity.getQuantity());
        dto.setPrice(entity.getPrice());
        dto.setSubtotal(entity.getQuantity() * entity.getPrice());
        return dto;
    }
}
