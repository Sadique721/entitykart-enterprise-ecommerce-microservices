package com.entitykart.commonservices.notification.controller;

import com.entitykart.commonservices.notification.entity.NotificationEntity;
import com.entitykart.commonservices.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin REST controller for managing notification history.
 * Originally from notification-service — all endpoints preserved.
 * All endpoints require ROLE_ADMIN (enforced by the embedded Gateway filter).
 *
 * Base URL: /api/admin/notifications
 */
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** GET /api/admin/notifications — All notifications */
    @GetMapping
    public ResponseEntity<List<NotificationEntity>> getAll() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    /** GET /api/admin/notifications/user/{userId} — Notifications for a specific user */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationEntity>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getByUser(userId));
    }

    /** GET /api/admin/notifications/failed — All failed notifications */
    @GetMapping("/failed")
    public ResponseEntity<List<NotificationEntity>> getFailed() {
        return ResponseEntity.ok(notificationService.getFailedNotifications());
    }

    /** POST /api/admin/notifications/{id}/retry — Retry a failed notification */
    @PostMapping("/{id}/retry")
    public ResponseEntity<Map<String, String>> retry(@PathVariable Long id) {
        notificationService.retryFailed(id);
        return ResponseEntity.ok(Map.of("message", "Retry attempted for notification: " + id));
    }

    /** POST /api/admin/notifications/welcome — Trigger welcome email manually */
    @PostMapping("/welcome")
    public ResponseEntity<Map<String, String>> sendWelcome(
            @RequestParam Long userId,
            @RequestParam String email,
            @RequestParam String name) {
        notificationService.handleWelcome(userId, email, name);
        return ResponseEntity.ok(Map.of("message", "Welcome email triggered for " + email));
    }
}
