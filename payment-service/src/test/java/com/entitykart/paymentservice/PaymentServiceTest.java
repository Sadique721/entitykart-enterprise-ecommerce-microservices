package com.entitykart.paymentservice;

import com.entitykart.paymentservice.client.OrderServiceClient;
import com.entitykart.paymentservice.dto.PaymentRequest;
import com.entitykart.paymentservice.entity.PaymentEntity;
import com.entitykart.paymentservice.repository.PaymentRepository;
import com.entitykart.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentEntity testPayment;
    private PaymentRequest testRequest;

    @BeforeEach
    public void setup() {
        // Set environment property fields for Mock mode using ReflectionTestUtils
        ReflectionTestUtils.setField(paymentService, "environment", "test");
        ReflectionTestUtils.setField(paymentService, "apiLoginId", "dummyId");
        ReflectionTestUtils.setField(paymentService, "transactionKey", "dummyKey");

        testPayment = new PaymentEntity();
        testPayment.setPaymentId(1L);
        testPayment.setOrderId(10L);
        testPayment.setAmount(500.0);
        testPayment.setPaymentMode(PaymentEntity.PaymentMode.CARD);
        testPayment.setPaymentStatus(PaymentEntity.PaymentStatus.PENDING);
        testPayment.setTransactionRef("TXN_12345");
        testPayment.setPaymentDate(LocalDateTime.now());

        testRequest = new PaymentRequest();
        testRequest.setOrderId(10L);
        testRequest.setAmount(500.0);
        testRequest.setCardNumber("1111222233334444");
        testRequest.setExpiryMonth("12");
        testRequest.setExpiryYear("29");
        testRequest.setCvv("123");
        testRequest.setCustomerEmail("customer@example.com");
        testRequest.setCustomerName("Demo User");
    }

    // --- CARD PAYMENT PROCESSING TESTS (5 Tests) ---

    @Test
    public void testProcessCardPayment_MockMode_Success() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(i -> {
            PaymentEntity p = (PaymentEntity) i.getArguments()[0];
            p.setPaymentId(1L);
            return p;
        });
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        doNothing().when(orderServiceClient).updateOrderPaymentStatus(10L, "PAID");

        PaymentEntity result = paymentService.processCardPayment(testRequest);

        assertNotNull(result);
        assertEquals(PaymentEntity.PaymentStatus.SUCCESS, result.getPaymentStatus());
        assertTrue(result.getTransactionRef().startsWith("MOCK_CARD_"));
        verify(kafkaTemplate, times(1)).send(eq("payment-events"), any());
    }

    @Test
    public void testProcessCardPayment_AlreadyPaid_ThrowsException() {
        testPayment.setPaymentStatus(PaymentEntity.PaymentStatus.SUCCESS);
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(testPayment));

        assertThrows(RuntimeException.class, () -> paymentService.processCardPayment(testRequest));
    }

    @Test
    public void testCheckAndCreateInitialPayment_ExistingPending() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(testPayment));

        PaymentEntity initial = paymentService.checkAndCreateInitialPayment(testRequest);

        assertNotNull(initial);
        assertEquals(1L, initial.getPaymentId());
        assertEquals(PaymentEntity.PaymentStatus.PENDING, initial.getPaymentStatus());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    public void testCheckAndCreateInitialPayment_NewRecord() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(testPayment);

        PaymentEntity initial = paymentService.checkAndCreateInitialPayment(testRequest);

        assertNotNull(initial);
        verify(paymentRepository, times(1)).save(any(PaymentEntity.class));
    }

    @Test
    public void testUpdatePaymentStatusAndPublish_Success() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        doNothing().when(orderServiceClient).updateOrderPaymentStatus(10L, "PAID");

        PaymentEntity updated = paymentService.updatePaymentStatusAndPublish(1L, PaymentEntity.PaymentStatus.SUCCESS, "TXN_OK", "Approved", "user@example.com", "User");

        assertNotNull(updated);
        assertEquals(PaymentEntity.PaymentStatus.SUCCESS, updated.getPaymentStatus());
        assertEquals("TXN_OK", updated.getTransactionRef());
        verify(kafkaTemplate, times(1)).send(eq("payment-events"), any());
    }

    // --- OFFLINE / ALTERNATIVE CHANNELS TESTS (6 Tests) ---

    @Test
    public void testProcessOfflinePayment_UPI_Success() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        doNothing().when(orderServiceClient).updateOrderPaymentStatus(10L, "PAID");

        PaymentEntity result = paymentService.processOfflinePayment(10L, 500.0, "UPI", "user@example.com", "User");

        assertNotNull(result);
        assertEquals(PaymentEntity.PaymentStatus.SUCCESS, result.getPaymentStatus());
        assertTrue(result.getTransactionRef().startsWith("UPI_"));
    }

    @Test
    public void testProcessOfflinePayment_COD_Pending() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        PaymentEntity result = paymentService.processOfflinePayment(10L, 500.0, "COD", "user@example.com", "User");

        assertNotNull(result);
        assertEquals(PaymentEntity.PaymentStatus.PENDING, result.getPaymentStatus());
        assertTrue(result.getTransactionRef().startsWith("COD_PENDING_"));
    }

    @Test
    public void testProcessOfflinePayment_AlreadyPaid_ThrowsException() {
        testPayment.setPaymentStatus(PaymentEntity.PaymentStatus.SUCCESS);
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(testPayment));

        assertThrows(RuntimeException.class, () -> paymentService.processOfflinePayment(10L, 500.0, "UPI"));
    }

    @Test
    public void testProcessNetBankingPayment_Success() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        PaymentEntity result = paymentService.processNetBankingPayment(10L, 500.0, "HDFC", "user@example.com", "User");

        assertNotNull(result);
        assertEquals(PaymentEntity.PaymentStatus.SUCCESS, result.getPaymentStatus());
        assertTrue(result.getTransactionRef().startsWith("NB_HDFC_"));
    }

    @Test
    public void testProcessWalletPayment_Success() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        PaymentEntity result = paymentService.processWalletPayment(10L, 500.0, "PAYTM", "user@example.com", "User");

        assertNotNull(result);
        assertEquals(PaymentEntity.PaymentStatus.SUCCESS, result.getPaymentStatus());
        assertTrue(result.getTransactionRef().startsWith("WLT_PAYTM_"));
    }

    @Test
    public void testProcessEmiPayment_Success() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        PaymentEntity result = paymentService.processEmiPayment(10L, 500.0, "1111222233334444", 6, "user@example.com", "User");

        assertNotNull(result);
        assertEquals(PaymentEntity.PaymentStatus.SUCCESS, result.getPaymentStatus());
        assertTrue(result.getTransactionRef().startsWith("EMI_6M_"));
    }

    // --- COD ASSIGNMENT & READS TESTS (4 Tests) ---

    @Test
    public void testAssignCodTransaction_Success() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        doNothing().when(orderServiceClient).updateOrderPaymentStatus(10L, "PAID");

        PaymentEntity result = paymentService.assignCodTransaction(10L);

        assertNotNull(result);
        assertEquals(PaymentEntity.PaymentStatus.SUCCESS, result.getPaymentStatus());
        assertTrue(result.getTransactionRef().startsWith("COD_DELIVERED_10_"));
    }

    @Test
    public void testGetPaymentByOrderId_Success() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(testPayment));

        PaymentEntity result = paymentService.getPaymentByOrderId(10L);

        assertNotNull(result);
        assertEquals(10L, result.getOrderId());
    }

    @Test
    public void testGetPaymentByOrderId_NotFound() {
        when(paymentRepository.findByOrderId(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentService.getPaymentByOrderId(99L));
    }

    @Test
    public void testGetAllPayments_Success() {
        when(paymentRepository.findAll()).thenReturn(List.of(testPayment));

        List<PaymentEntity> list = paymentService.getAllPayments();

        assertNotNull(list);
        assertEquals(1, list.size());
    }
}
