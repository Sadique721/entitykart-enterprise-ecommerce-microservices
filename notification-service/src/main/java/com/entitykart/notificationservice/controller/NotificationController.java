package com.entitykart.notificationservice.controller;

import com.entitykart.notificationservice.entity.NotificationEntity;
import com.entitykart.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoint to monitor and manage notification history.
 * All endpoints require ROLE_ADMIN (enforced by API Gateway).
 */
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** GET /api/admin/notifications – All notifications */
    @GetMapping
    public ResponseEntity<List<NotificationEntity>> getAll() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    /** GET /api/admin/notifications/user/{userId} – Notifications for a user */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationEntity>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getByUser(userId));
    }

    /** GET /api/admin/notifications/failed – All failed notifications */
    @GetMapping("/failed")
    public ResponseEntity<List<NotificationEntity>> getFailed() {
        return ResponseEntity.ok(notificationService.getFailedNotifications());
    }

    /** POST /api/admin/notifications/{id}/retry – Retry a failed notification */
    @PostMapping("/{id}/retry")
    public ResponseEntity<Map<String, String>> retry(@PathVariable Long id) {
        notificationService.retryFailed(id);
        return ResponseEntity.ok(Map.of("message", "Retry attempted for notification: " + id));
    }

    /** POST /api/notifications/welcome – Trigger welcome email (internal use) */
    @PostMapping("/welcome")
    public ResponseEntity<Map<String, String>> sendWelcome(
            @RequestParam Long userId,
            @RequestParam String email,
            @RequestParam String name) {
        notificationService.handleWelcome(userId, email, name);
        return ResponseEntity.ok(Map.of("message", "Welcome email triggered for " + email));
    }
}
