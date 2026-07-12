package com.entitykart.orderservice;

import com.entitykart.orderservice.client.UserServiceClient;
import com.entitykart.orderservice.dto.OrderDTO;
import com.entitykart.orderservice.entity.OrderEntity;
import com.entitykart.orderservice.entity.OrderItemEntity;
import com.entitykart.orderservice.repository.OrderItemRepository;
import com.entitykart.orderservice.repository.OrderRepository;
import com.entitykart.orderservice.service.OrderService;
import com.entitykart.shared.dto.CartCheckoutEvent;
import com.entitykart.shared.dto.CartItemDTO;
import com.entitykart.shared.dto.OrderPlacedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private OrderService orderService;

    private OrderEntity testOrder;
    private OrderItemEntity testItem;

    @BeforeEach
    public void setup() {
        testOrder = new OrderEntity();
        testOrder.setOrderId(1L);
        testOrder.setCustomerId(10L);
        testOrder.setAddressId(20L);
        testOrder.setTotalAmount(500.0);
        testOrder.setOrderStatus(OrderEntity.OrderStatus.PENDING_PAYMENT);
        testOrder.setPaymentStatus(OrderEntity.PaymentStatus.UNPAID);
        testOrder.setOrderDate(LocalDateTime.now());

        testItem = new OrderItemEntity();
        testItem.setOrderItemId(100L);
        testItem.setOrderId(1L);
        testItem.setProductId(101L);
        testItem.setQuantity(2);
        testItem.setPrice(250.0);
    }

    // --- CREATE ORDER TESTS (6 Tests) ---

    @Test
    public void testCreateOrder_Success() {
        CartItemDTO item = new CartItemDTO(101L, 2, 250.0);
        CartCheckoutEvent event = new CartCheckoutEvent(
                10L, 20L, Arrays.asList(item), 500.0, "CARD"
        );

        UserServiceClient.UserInfo user = new UserServiceClient.UserInfo();
        user.setEmail("user@example.com");
        user.setName("John User");

        when(orderRepository.save(any(OrderEntity.class))).thenReturn(testOrder);
        when(userServiceClient.getUser(10L)).thenReturn(user);

        OrderDTO created = orderService.createOrder(event);

        assertNotNull(created);
        assertEquals(10L, created.getCustomerId());
        assertEquals(20L, created.getAddressId());
        assertEquals(500.0, created.getTotalAmount());
        verify(orderItemRepository, times(1)).saveAll(any());
        verify(kafkaTemplate, times(1)).send(eq("order-events"), any(OrderPlacedEvent.class));
    }

    @Test
    public void testCreateOrder_UserServiceClientOutage_SucceedsWithDefaults() {
        CartItemDTO item = new CartItemDTO(101L, 2, 250.0);
        CartCheckoutEvent event = new CartCheckoutEvent(
                10L, 20L, Arrays.asList(item), 500.0, "CARD"
        );

        when(orderRepository.save(any(OrderEntity.class))).thenReturn(testOrder);
        when(userServiceClient.getUser(10L)).thenThrow(new RuntimeException("Service offline"));

        OrderDTO created = orderService.createOrder(event);

        assertNotNull(created);
        verify(kafkaTemplate, times(1)).send(eq("order-events"), any(OrderPlacedEvent.class));
    }

    @Test
    public void testCreateOrder_EmptyCart_ThrowsException() {
        CartCheckoutEvent event = new CartCheckoutEvent(
                10L, 20L, Collections.emptyList(), 0.0, "CARD"
        );
        assertThrows(RuntimeException.class, () -> orderService.createOrder(event));
    }

    @Test
    public void testCreateOrder_NullCart_ThrowsException() {
        CartCheckoutEvent event = new CartCheckoutEvent(
                10L, 20L, null, 500.0, "CARD"
        );
        assertThrows(RuntimeException.class, () -> orderService.createOrder(event));
    }

    @Test
    public void testCreateOrder_KafkaFailure_DoesNotRollback() {
        CartItemDTO item = new CartItemDTO(101L, 2, 250.0);
        CartCheckoutEvent event = new CartCheckoutEvent(
                10L, 20L, Arrays.asList(item), 500.0, "CARD"
        );

        when(orderRepository.save(any(OrderEntity.class))).thenReturn(testOrder);
        doThrow(new RuntimeException("Kafka Broker Down")).when(kafkaTemplate).send(anyString(), any());

        OrderDTO created = orderService.createOrder(event);
        assertNotNull(created);
        assertEquals(10L, created.getCustomerId());
    }

    @Test
    public void testConvertItemToDTO_CorrectMath() {
        // Just checking how convertItemToDTO handles fields
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(testItem));

        OrderDTO fetched = orderService.getOrder(1L);
        assertNotNull(fetched);
        assertEquals(1, fetched.getItems().size());
        assertEquals(500.0, fetched.getItems().get(0).getSubtotal());
    }

    // --- QUERY TESTS (3 Tests) ---

    @Test
    public void testGetOrder_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());

        OrderDTO fetched = orderService.getOrder(1L);

        assertNotNull(fetched);
        assertEquals(1L, fetched.getOrderId());
    }

    @Test
    public void testGetOrder_NotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> orderService.getOrder(99L));
    }

    @Test
    public void testGetOrdersByCustomer_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderEntity> page = new PageImpl<>(List.of(testOrder), pageable, 1);
        when(orderRepository.findByCustomerIdOrderByOrderDateDesc(10L, pageable)).thenReturn(page);
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());

        Page<OrderDTO> result = orderService.getOrdersByCustomer(10L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    // --- ORDER STATUS UPDATES TESTS (4 Tests) ---

    @Test
    public void testUpdateOrderStatus_PlacedToConfirmed() {
        testOrder.setOrderStatus(OrderEntity.OrderStatus.PLACED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        orderService.updateOrderStatus(1L, "CONFIRMED");

        assertEquals(OrderEntity.OrderStatus.CONFIRMED, testOrder.getOrderStatus());
        verify(kafkaTemplate, times(1)).send(eq("order-events"), any(OrderPlacedEvent.class));
    }

    @Test
    public void testUpdateOrderStatus_ConfirmToShipped() {
        testOrder.setOrderStatus(OrderEntity.OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        orderService.updateOrderStatus(1L, "SHIPPED");

        assertEquals(OrderEntity.OrderStatus.SHIPPED, testOrder.getOrderStatus());
    }

    @Test
    public void testUpdateOrderStatus_ShippedToDelivered() {
        testOrder.setOrderStatus(OrderEntity.OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        orderService.updateOrderStatus(1L, "DELIVERED");

        assertEquals(OrderEntity.OrderStatus.DELIVERED, testOrder.getOrderStatus());
    }

    @Test
    public void testUpdateOrderStatus_InvalidStatusEnum() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThrows(RuntimeException.class, () -> orderService.updateOrderStatus(1L, "INVALID_STATUS"));
    }

    // --- PAYMENT STATUS UPDATES TESTS (3 Tests) ---

    @Test
    public void testUpdatePaymentStatus_Paid_UpdatesOrderStatusToPlaced() {
        testOrder.setOrderStatus(OrderEntity.OrderStatus.PENDING_PAYMENT);
        testOrder.setPaymentStatus(OrderEntity.PaymentStatus.UNPAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        orderService.updatePaymentStatus(1L, "PAID");

        assertEquals(OrderEntity.PaymentStatus.PAID, testOrder.getPaymentStatus());
        assertEquals(OrderEntity.OrderStatus.PLACED, testOrder.getOrderStatus());
    }

    @Test
    public void testUpdatePaymentStatus_Refunded() {
        testOrder.setPaymentStatus(OrderEntity.PaymentStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        orderService.updatePaymentStatus(1L, "REFUNDED");

        assertEquals(OrderEntity.PaymentStatus.REFUNDED, testOrder.getPaymentStatus());
    }

    @Test
    public void testUpdateOrderStatus_HandlesPaymentStatusUpdatesDirectly() {
        testOrder.setOrderStatus(OrderEntity.OrderStatus.PENDING_PAYMENT);
        testOrder.setPaymentStatus(OrderEntity.PaymentStatus.UNPAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        orderService.updateOrderStatus(1L, "PAID");

        assertEquals(OrderEntity.PaymentStatus.PAID, testOrder.getPaymentStatus());
        assertEquals(OrderEntity.OrderStatus.PLACED, testOrder.getOrderStatus());
    }
}
