package com.entitykart.commonservices.notification.listener;

import com.entitykart.commonservices.notification.dto.*;
import com.entitykart.commonservices.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka notification listener (originally from notification-service).
 * Consumes events from order-events, payment-events, return-events,
 * user-events, and password-reset-events topics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationListener {

    private final NotificationService notificationService;

    /** Listens to order-events topic — order placement and status changes. */
    @KafkaListener(topics = "order-events", groupId = "common-services-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderEvent(OrderPlacedEvent event) {
        log.info("Received order event: orderId={}, status={}", event.getOrderId(), event.getOrderStatus());
        try {
            if (event.getCustomerEmail() == null || event.getCustomerEmail().isBlank()) {
                log.warn("Order event missing customerEmail, skipping for orderId={}", event.getOrderId());
                return;
            }
            notificationService.handleOrderPlaced(
                    event.getOrderId(),
                    event.getCustomerId(),
                    event.getCustomerEmail(),
                    event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                    event.getTotalAmount()
            );
        } catch (Exception e) {
            log.error("Failed to process order event for orderId={}: {}", event.getOrderId(), e.getMessage());
        }
    }

    /** Listens to payment-events topic — payment success and failure. */
    @KafkaListener(topics = "payment-events", groupId = "common-services-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onPaymentEvent(PaymentProcessedEvent event) {
        log.info("Received payment event: orderId={}, status={}", event.getOrderId(), event.getStatus());
        try {
            if (event.getCustomerEmail() == null || event.getCustomerEmail().isBlank()) {
                log.warn("Payment event missing customerEmail, skipping for orderId={}", event.getOrderId());
                return;
            }
            String name = event.getCustomerName() != null ? event.getCustomerName() : "Customer";
            if ("SUCCESS".equalsIgnoreCase(event.getStatus())) {
                notificationService.handlePaymentSuccess(
                        event.getOrderId(),
                        null,
                        event.getCustomerEmail(),
                        name,
                        event.getTransactionRef(),
                        event.getAmount()
                );
            } else {
                notificationService.handlePaymentFailed(
                        event.getOrderId(), null, event.getCustomerEmail(), name);
            }
        } catch (Exception e) {
            log.error("Failed to process payment event for orderId={}: {}", event.getOrderId(), e.getMessage());
        }
    }

    /** Listens to return-events topic — return approval, rejection, refund. */
    @KafkaListener(topics = "return-events", groupId = "common-services-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onReturnEvent(ReturnApprovedEvent event) {
        log.info("Received return event: returnId={}, status={}", event.getReturnId(), event.getStatus());
        try {
            if (event.getCustomerEmail() == null || event.getCustomerEmail().isBlank()) {
                log.warn("Return event missing customerEmail, skipping for returnId={}", event.getReturnId());
                return;
            }
            notificationService.handleReturnStatusUpdate(
                    event.getReturnId(),
                    event.getCustomerId(),
                    event.getCustomerEmail(),
                    event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                    event.getStatus(),
                    event.getRefundAmount(),
                    null
            );
        } catch (Exception e) {
            log.error("Failed to process return event for returnId={}: {}", event.getReturnId(), e.getMessage());
        }
    }

    /** Listens to user-events topic — welcome emails for new registrations. */
    @KafkaListener(topics = "user-events", groupId = "common-services-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onUserCreatedEvent(UserCreatedEvent event) {
        log.info("Received user created event: id={}, email={}", event.getId(), event.getEmail());
        try {
            if (event.getEmail() == null || event.getEmail().isBlank()) {
                log.warn("User created event missing email, skipping");
                return;
            }
            notificationService.handleWelcome(
                    event.getId(),
                    event.getEmail(),
                    event.getName() != null ? event.getName() : "Customer"
            );
        } catch (Exception e) {
            log.error("Failed to process user created event: {}", e.getMessage());
        }
    }

    /** Listens to password-reset-events topic — password reset email with token. */
    @KafkaListener(topics = "password-reset-events", groupId = "common-services-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onPasswordResetEvent(PasswordResetEvent event) {
        log.info("Received password reset event: id={}, email={}", event.getId(), event.getEmail());
        try {
            if (event.getEmail() == null || event.getEmail().isBlank()) {
                log.warn("Password reset event missing email, skipping");
                return;
            }
            notificationService.handlePasswordReset(
                    event.getId(),
                    event.getEmail(),
                    event.getName() != null ? event.getName() : "Customer",
                    event.getToken()
            );
        } catch (Exception e) {
            log.error("Failed to process password reset event: {}", e.getMessage());
        }
    }
}
