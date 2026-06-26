package com.entitykart.commonservices.notification.service;

import com.entitykart.commonservices.notification.entity.NotificationEntity;
import com.entitykart.commonservices.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Notification orchestration service (originally from notification-service).
 * Persists notification records and triggers async email delivery.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    /**
     * Core method: persist notification record with SENT status, then fire
     * async email via @Async sendHtmlEmail (fire-and-forget).
     *
     * We mark it SENT optimistically before sending — the @Async method will
     * log errors if SMTP fails. Use retryFailed() from admin panel to resend.
     */
    @Transactional
    public void sendAndSave(Long userId, String email, String subject,
                             String htmlBody, NotificationEntity.NotificationType type) {
        NotificationEntity notification = new NotificationEntity();
        notification.setUserId(userId);
        notification.setEmail(email);
        notification.setSubject(subject);
        notification.setMessage(htmlBody);
        notification.setType(type);
        notification.setStatus(NotificationEntity.NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);

        // Fire-and-forget: @Async ensures Kafka listener is not blocked
        emailService.sendHtmlEmail(email, subject, htmlBody);

        log.info("Notification queued: type={}, to={}, subject={}", type, email, subject);
    }


    // ─── Event Handlers ───────────────────────────────────────────────────────

    public void handleOrderPlaced(Long orderId, Long customerId, String email,
                                   String name, Double total) {
        String subject = "✅ Order #" + orderId + " Confirmed – EntityKart";
        String body = emailService.buildOrderPlacedEmail(name, orderId, total);
        sendAndSave(customerId, email, subject, body, NotificationEntity.NotificationType.ORDER_PLACED);
    }

    public void handleOrderConfirmed(Long orderId, Long customerId, String email,
                                     String name, Double total) {
        String subject = "📦 Order #" + orderId + " has been Confirmed – EntityKart";
        String body = emailService.buildOrderConfirmedEmail(name, orderId, total);
        sendAndSave(customerId, email, subject, body, NotificationEntity.NotificationType.ORDER_CONFIRMED);
    }

    public void handleOrderShipped(Long orderId, Long customerId, String email,
                                   String name, Double total) {
        String subject = "🚚 Order #" + orderId + " Shipped! It's on its way – EntityKart";
        String body = emailService.buildOrderShippedEmail(name, orderId, total);
        sendAndSave(customerId, email, subject, body, NotificationEntity.NotificationType.ORDER_SHIPPED);
    }

    public void handleOrderDelivered(Long orderId, Long customerId, String email,
                                     String name, Double total) {
        String subject = "🎉 Order #" + orderId + " Delivered Successfully – EntityKart";
        String body = emailService.buildOrderDeliveredEmail(name, orderId, total);
        sendAndSave(customerId, email, subject, body, NotificationEntity.NotificationType.ORDER_DELIVERED);
    }

    public void handleOrderCancelled(Long orderId, Long customerId, String email,
                                     String name, Double total) {
        String subject = "❌ Order #" + orderId + " Cancelled – EntityKart";
        String body = emailService.buildOrderCancelledEmail(name, orderId, total);
        sendAndSave(customerId, email, subject, body, NotificationEntity.NotificationType.ORDER_CANCELLED);
    }

    public void handleOrderReturned(Long orderId, Long customerId, String email,
                                    String name, Double total) {
        String subject = "📦 Return for Order #" + orderId + " Initiated – EntityKart";
        String body = emailService.buildOrderReturnedEmail(name, orderId, total);
        sendAndSave(customerId, email, subject, body, NotificationEntity.NotificationType.RETURN_REQUESTED);
    }



    public void handlePaymentSuccess(Long orderId, Long customerId, String email,
                                      String name, String txnRef, Double amount) {
        String subject = "💳 Payment Confirmed for Order #" + orderId + " – EntityKart";
        String body = emailService.buildPaymentSuccessEmail(name, orderId, txnRef, amount);
        sendAndSave(customerId, email, subject, body, NotificationEntity.NotificationType.PAYMENT_SUCCESS);
    }

    public void handlePaymentFailed(Long orderId, Long customerId, String email, String name) {
        String subject = "⚠️ Payment Failed for Order #" + orderId + " – EntityKart";
        String body = emailService.buildPaymentFailedEmail(name, orderId);
        sendAndSave(customerId, email, subject, body, NotificationEntity.NotificationType.PAYMENT_FAILED);
    }

    public void handleReturnStatusUpdate(Long returnId, Long customerId, String email,
                                          String name, String status,
                                          Double refundAmount, String rejectionReason) {
        NotificationEntity.NotificationType type = resolveReturnType(status);
        String subject = buildReturnSubject(returnId, status);
        String body = emailService.buildReturnStatusEmail(name, returnId, status, refundAmount, rejectionReason);
        sendAndSave(customerId, email, subject, body, type);
    }

    public void handleWelcome(Long userId, String email, String name) {
        String subject = "🎉 Welcome to EntityKart, " + name + "!";
        String body = emailService.buildWelcomeEmail(name);
        sendAndSave(userId, email, subject, body, NotificationEntity.NotificationType.WELCOME);
    }

    public void handlePasswordReset(Long userId, String email, String name, String token) {
        String subject = "🔑 Reset Your EntityKart Password";
        String body = emailService.buildPasswordResetEmail(name, token);
        sendAndSave(userId, email, subject, body, NotificationEntity.NotificationType.PASSWORD_RESET);
    }

    // ─── Admin Queries ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationEntity> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<NotificationEntity> getByUser(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationEntity> getFailedNotifications() {
        return notificationRepository.findByStatus(NotificationEntity.NotificationStatus.FAILED);
    }

    @Transactional
    public void retryFailed(Long notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        if (notification.getStatus() != NotificationEntity.NotificationStatus.FAILED) {
            throw new RuntimeException("Only FAILED notifications can be retried");
        }
        // Mark as SENT optimistically, then fire async email
        notification.setStatus(NotificationEntity.NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notification.setErrorMessage(null);
        notificationRepository.save(notification);
        emailService.sendHtmlEmail(
                notification.getEmail(), notification.getSubject(), notification.getMessage());
        log.info("Retrying notification id={} to {}", notificationId, notification.getEmail());
    }


    // ─── Helpers ──────────────────────────────────────────────────────────────

    private NotificationEntity.NotificationType resolveReturnType(String status) {
        return switch (status.toUpperCase()) {
            case "APPROVED"  -> NotificationEntity.NotificationType.RETURN_APPROVED;
            case "REJECTED"  -> NotificationEntity.NotificationType.RETURN_REJECTED;
            case "REFUNDED"  -> NotificationEntity.NotificationType.REFUND_PROCESSED;
            default          -> NotificationEntity.NotificationType.RETURN_REQUESTED;
        };
    }

    private String buildReturnSubject(Long returnId, String status) {
        return switch (status.toUpperCase()) {
            case "APPROVED" -> "✅ Return #" + returnId + " Approved – EntityKart";
            case "REJECTED" -> "❌ Return #" + returnId + " Rejected – EntityKart";
            case "REFUNDED" -> "💰 Refund Initiated for Return #" + returnId + " – EntityKart";
            default         -> "📦 Return #" + returnId + " Update – EntityKart";
        };
    }
}
