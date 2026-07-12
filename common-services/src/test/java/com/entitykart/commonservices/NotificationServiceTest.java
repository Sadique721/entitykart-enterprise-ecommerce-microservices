package com.entitykart.commonservices;

import com.entitykart.commonservices.notification.entity.NotificationEntity;
import com.entitykart.commonservices.notification.repository.NotificationRepository;
import com.entitykart.commonservices.notification.service.EmailService;
import com.entitykart.commonservices.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationEntity testNotification;

    @BeforeEach
    public void setup() {
        testNotification = new NotificationEntity();
        testNotification.setNotificationId(1L);
        testNotification.setUserId(10L);
        testNotification.setEmail("user@example.com");
        testNotification.setSubject("Test Subject");
        testNotification.setMessage("Test Body");
        testNotification.setType(NotificationEntity.NotificationType.WELCOME);
        testNotification.setStatus(NotificationEntity.NotificationStatus.FAILED);
        testNotification.setCreatedAt(LocalDateTime.now());
    }

    // --- CORE & EVENT HANDLER TESTS (11 Tests) ---

    @Test
    public void testSendAndSave_Success() {
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());

        notificationService.sendAndSave(10L, "user@example.com", "Test Subject", "Test Body", NotificationEntity.NotificationType.WELCOME);

        verify(notificationRepository, times(1)).save(any(NotificationEntity.class));
        verify(emailService, times(1)).sendHtmlEmail("user@example.com", "Test Subject", "Test Body");
    }

    @Test
    public void testHandleWelcome() {
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        when(emailService.buildWelcomeEmail("John")).thenReturn("Welcome John");
        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());

        notificationService.handleWelcome(10L, "user@example.com", "John");

        verify(notificationRepository, times(1)).save(any(NotificationEntity.class));
        verify(emailService, times(1)).buildWelcomeEmail("John");
    }

    @Test
    public void testHandlePasswordReset() {
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        when(emailService.buildPasswordResetEmail("John", "token123")).thenReturn("Reset Body");
        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());

        notificationService.handlePasswordReset(10L, "user@example.com", "John", "token123");

        verify(emailService, times(1)).buildPasswordResetEmail("John", "token123");
    }

    @Test
    public void testHandleOrderPlaced() {
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        when(emailService.buildOrderPlacedEmail("John", 100L, 50.0)).thenReturn("Placed Body");
        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());

        notificationService.handleOrderPlaced(100L, 10L, "user@example.com", "John", 50.0);

        verify(emailService, times(1)).buildOrderPlacedEmail("John", 100L, 50.0);
    }

    @Test
    public void testHandleOrderConfirmed() {
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        when(emailService.buildOrderConfirmedEmail("John", 100L, 50.0)).thenReturn("Confirmed Body");
        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());

        notificationService.handleOrderConfirmed(100L, 10L, "user@example.com", "John", 50.0);

        verify(emailService, times(1)).buildOrderConfirmedEmail("John", 100L, 50.0);
    }

    @Test
    public void testHandleOrderShipped() {
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        when(emailService.buildOrderShippedEmail("John", 100L, 50.0)).thenReturn("Shipped Body");
        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());

        notificationService.handleOrderShipped(100L, 10L, "user@example.com", "John", 50.0);

        verify(emailService, times(1)).buildOrderShippedEmail("John", 100L, 50.0);
    }

    @Test
    public void testHandleOrderDelivered() {
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        when(emailService.buildOrderDeliveredEmail("John", 100L, 50.0)).thenReturn("Delivered Body");
        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());

        notificationService.handleOrderDelivered(100L, 10L, "user@example.com", "John", 50.0);

        verify(emailService, times(1)).buildOrderDeliveredEmail("John", 100L, 50.0);
    }

    @Test
    public void testHandleOrderCancelled() {
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        when(emailService.buildOrderCancelledEmail("John", 100L, 50.0)).thenReturn("Cancelled Body");
        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());

        notificationService.handleOrderCancelled(100L, 10L, "user@example.com", "John", 50.0);

        verify(emailService, times(1)).buildOrderCancelledEmail("John", 100L, 50.0);
    }

    @Test
    public void testHandleOrderReturned() {
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        when(emailService.buildOrderReturnedEmail("John", 100L, 50.0)).thenReturn("Returned Body");
        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());

        notificationService.handleOrderReturned(100L, 10L, "user@example.com", "John", 50.0);

        verify(emailService, times(1)).buildOrderReturnedEmail("John", 100L, 50.0);
    }

    @Test
    public void testHandlePaymentSuccess() {
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        when(emailService.buildPaymentSuccessEmail("John", 100L, "REF1", 50.0)).thenReturn("Payment Success Body");
        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());

        notificationService.handlePaymentSuccess(100L, 10L, "user@example.com", "John", "REF1", 50.0);

        verify(emailService, times(1)).buildPaymentSuccessEmail("John", 100L, "REF1", 50.0);
    }

    @Test
    public void testHandlePaymentFailed() {
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        when(emailService.buildPaymentFailedEmail("John", 100L)).thenReturn("Payment Failed Body");
        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());

        notificationService.handlePaymentFailed(100L, 10L, "user@example.com", "John");

        verify(emailService, times(1)).buildPaymentFailedEmail("John", 100L);
    }

    // --- ADMIN & QUERY TESTS (6 Tests) ---

    @Test
    public void testGetAllNotifications() {
        when(notificationRepository.findAll()).thenReturn(List.of(testNotification));

        List<NotificationEntity> result = notificationService.getAllNotifications();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testGetByUser() {
        when(notificationRepository.findByUserId(10L)).thenReturn(List.of(testNotification));

        List<NotificationEntity> result = notificationService.getByUser(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testGetFailedNotifications() {
        when(notificationRepository.findByStatus(NotificationEntity.NotificationStatus.FAILED)).thenReturn(List.of(testNotification));

        List<NotificationEntity> result = notificationService.getFailedNotifications();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testRetryFailed_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());

        notificationService.retryFailed(1L);

        assertEquals(NotificationEntity.NotificationStatus.SENT, testNotification.getStatus());
        verify(emailService, times(1)).sendHtmlEmail("user@example.com", "Test Subject", "Test Body");
    }

    @Test
    public void testRetryFailed_AlreadySent_ThrowsException() {
        testNotification.setStatus(NotificationEntity.NotificationStatus.SENT);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        assertThrows(RuntimeException.class, () -> notificationService.retryFailed(1L));
    }

    @Test
    public void testRetryFailed_NotFound_ThrowsException() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> notificationService.retryFailed(99L));
    }
}
