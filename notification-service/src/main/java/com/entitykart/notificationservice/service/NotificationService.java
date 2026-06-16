package com.entitykart.notificationservice.service;

import com.entitykart.notificationservice.entity.NotificationEntity;
import com.entitykart.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    /**
     * Core method: persist notification record, attempt email send, update status.
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
        notification.setStatus(NotificationEntity.NotificationStatus.PENDING);
        NotificationEntity saved = notificationRepository.save(notification);

        // Send email asynchronously via EmailService
        boolean success = emailService.sendHtmlEmail(email, subject, htmlBody);

        // Update status synchronously after async call returns result
        if (success) {
            saved.setStatus(NotificationEntity.NotificationStatus.SENT);
            saved.setSentAt(LocalDateTime.now());
        } else {
            saved.setStatus(NotificationEntity.NotificationStatus.FAILED);
            saved.setErrorMessage("SMTP delivery failed");
        }
        notificationRepository.save(saved);
    }

    // ─── Event Handlers ───────────────────────────────────────────────────────

    public void handleOrderPlaced(Long orderId, Long customerId, String email,
                                   String name, Double total) {
        String subject = "✅ Order #" + orderId + " Confirmed – EntityKart";
        String body = emailService.buildOrderPlacedEmail(name, orderId, total);
        sendAndSave(customerId, email, subject, body, NotificationEntity.NotificationType.ORDER_PLACED);
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
        boolean success = emailService.sendHtmlEmail(
                notification.getEmail(), notification.getSubject(), notification.getMessage());
        if (success) {
            notification.setStatus(NotificationEntity.NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setErrorMessage(null);
        } else {
            notification.setErrorMessage("Retry failed at " + LocalDateTime.now());
        }
        notificationRepository.save(notification);
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
