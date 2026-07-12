package com.entitykart.returnservice;

import com.entitykart.returnservice.client.OrderServiceClient;
import com.entitykart.returnservice.dto.*;
import com.entitykart.returnservice.entity.ReturnEntity;
import com.entitykart.returnservice.repository.ReturnRepository;
import com.entitykart.returnservice.service.RefundProcessor;
import com.entitykart.returnservice.service.ReturnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReturnServiceTest {

    @Mock
    private ReturnRepository returnRepository;

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private RefundProcessor refundProcessor;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ReturnService returnService;

    private ReturnEntity testReturn;
    private OrderDTO testOrder;
    private ReturnRequest testRequest;

    @BeforeEach
    public void setup() {
        testReturn = new ReturnEntity();
        testReturn.setReturnId(1L);
        testReturn.setOrderId(10L);
        testReturn.setCustomerId(100L);
        testReturn.setProductId(1001L);
        testReturn.setQuantity(1);
        testReturn.setReason("Damaged item");
        testReturn.setStatus(ReturnEntity.ReturnStatus.PENDING);
        testReturn.setRefundAmount(50.0);

        testOrder = new OrderDTO();
        testOrder.setOrderId(10L);
        testOrder.setCustomerId(100L);
        testOrder.setOrderStatus("DELIVERED");
        testOrder.setOrderDate(LocalDateTime.now().minusDays(10)); // Within 30 days

        OrderDTO.OrderItemDTO item = new OrderDTO.OrderItemDTO();
        item.setProductId(1001L);
        item.setQuantity(2);
        item.setPrice(50.0);
        testOrder.setItems(List.of(item));

        testRequest = new ReturnRequest();
        testRequest.setOrderId(10L);
        testRequest.setProductId(1001L);
        testRequest.setQuantity(1);
        testRequest.setReason("Damaged item");
    }

    // --- CREATE & ELIGIBILITY WINDOW TESTS (7 Tests) ---

    @Test
    public void testCreateReturn_Success() {
        when(orderServiceClient.getOrder(10L)).thenReturn(testOrder);
        when(returnRepository.existsByOrderIdAndProductIdAndStatusNot(10L, 1001L, ReturnEntity.ReturnStatus.REJECTED)).thenReturn(false);
        when(returnRepository.save(any(ReturnEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        ReturnResponse response = returnService.createReturn(100L, testRequest);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertEquals(50.0, response.getRefundAmount());
        verify(returnRepository, times(1)).save(any(ReturnEntity.class));
    }

    @Test
    public void testCreateReturn_OrderNotDelivered() {
        testOrder.setOrderStatus("SHIPPED");
        when(orderServiceClient.getOrder(10L)).thenReturn(testOrder);

        assertThrows(RuntimeException.class, () -> returnService.createReturn(100L, testRequest));
    }

    @Test
    public void testCreateReturn_WindowExceeded_Over30Days() {
        testOrder.setOrderDate(LocalDateTime.now().minusDays(31)); // Over 30 days
        when(orderServiceClient.getOrder(10L)).thenReturn(testOrder);

        assertThrows(RuntimeException.class, () -> returnService.createReturn(100L, testRequest));
    }

    @Test
    public void testCreateReturn_MismatchCustomer() {
        when(orderServiceClient.getOrder(10L)).thenReturn(testOrder);

        assertThrows(RuntimeException.class, () -> returnService.createReturn(999L, testRequest));
    }

    @Test
    public void testCreateReturn_QuantityExceeds() {
        testRequest.setQuantity(3); // Ordered only 2
        when(orderServiceClient.getOrder(10L)).thenReturn(testOrder);

        assertThrows(RuntimeException.class, () -> returnService.createReturn(100L, testRequest));
    }

    @Test
    public void testCreateReturn_ProductNotInOrder() {
        testRequest.setProductId(9999L);
        when(orderServiceClient.getOrder(10L)).thenReturn(testOrder);

        assertThrows(RuntimeException.class, () -> returnService.createReturn(100L, testRequest));
    }

    @Test
    public void testCreateReturn_DuplicateRequest() {
        when(orderServiceClient.getOrder(10L)).thenReturn(testOrder);
        when(returnRepository.existsByOrderIdAndProductIdAndStatusNot(10L, 1001L, ReturnEntity.ReturnStatus.REJECTED)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> returnService.createReturn(100L, testRequest));
    }

    // --- ADMIN DECISION & REFUND TESTS (6 Tests) ---

    @Test
    public void testProcessAdminDecision_Approve_Success() {
        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));
        when(returnRepository.save(any(ReturnEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        doNothing().when(orderServiceClient).updateOrderStatus(10L, "RETURNED");
        doNothing().when(refundProcessor).processRefund(any());

        AdminDecisionRequest decision = new AdminDecisionRequest();
        decision.setDecision("APPROVED");
        decision.setRefundAmount(50.0);
        decision.setAdminNote("Approved note");

        ReturnResponse response = returnService.processAdminDecision(1L, decision);

        assertEquals("APPROVED", response.getStatus());
        assertEquals("Approved note", response.getAdminNote());
        verify(kafkaTemplate, times(1)).send(eq("return-events"), any());
    }

    @Test
    public void testProcessAdminDecision_Reject_Success() {
        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));
        when(returnRepository.save(any(ReturnEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        AdminDecisionRequest decision = new AdminDecisionRequest();
        decision.setDecision("REJECTED");
        decision.setRejectionReason("Not eligible");

        ReturnResponse response = returnService.processAdminDecision(1L, decision);

        assertEquals("REJECTED", response.getStatus());
        assertEquals("Not eligible", response.getRejectionReason());
        verify(kafkaTemplate, times(1)).send(eq("return-events"), any());
    }

    @Test
    public void testProcessAdminDecision_NotPending() {
        testReturn.setStatus(ReturnEntity.ReturnStatus.APPROVED);
        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));

        AdminDecisionRequest decision = new AdminDecisionRequest();
        decision.setDecision("APPROVED");

        assertThrows(RuntimeException.class, () -> returnService.processAdminDecision(1L, decision));
    }

    @Test
    public void testProcessAdminDecision_InvalidDecision() {
        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));

        AdminDecisionRequest decision = new AdminDecisionRequest();
        decision.setDecision("INVALID");

        assertThrows(RuntimeException.class, () -> returnService.processAdminDecision(1L, decision));
    }

    @Test
    public void testProcessManualRefund_Success() {
        testReturn.setStatus(ReturnEntity.ReturnStatus.APPROVED);
        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));
        doNothing().when(refundProcessor).processRefund(testReturn);

        ReturnResponse response = returnService.processManualRefund(1L);

        assertNotNull(response);
    }

    @Test
    public void testProcessManualRefund_NotApproved() {
        testReturn.setStatus(ReturnEntity.ReturnStatus.PENDING);
        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));

        assertThrows(RuntimeException.class, () -> returnService.processManualRefund(1L));
    }

    // --- QUERY TESTS (3 Tests) ---

    @Test
    public void testGetReturnById_Success() {
        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));

        ReturnResponse response = returnService.getReturnById(1L, 100L);

        assertNotNull(response);
        assertEquals(1L, response.getReturnId());
    }

    @Test
    public void testGetReturnById_Unauthorized() {
        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));

        assertThrows(RuntimeException.class, () -> returnService.getReturnById(1L, 999L));
    }

    @Test
    public void testGetReturnsByCustomer() {
        when(returnRepository.findByCustomerId(100L)).thenReturn(List.of(testReturn));

        List<ReturnResponse> list = returnService.getReturnsByCustomer(100L);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(100L, list.get(0).getCustomerId());
    }
}
