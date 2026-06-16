package com.entitykart.returnservice.controller;

import com.entitykart.returnservice.dto.AdminDecisionRequest;
import com.entitykart.returnservice.dto.ReturnResponse;
import com.entitykart.returnservice.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only return management endpoints.
 * All endpoints under /api/admin/returns require ROLE_ADMIN via API Gateway.
 */
@RestController
@RequestMapping("/api/admin/returns")
@RequiredArgsConstructor
public class AdminReturnController {

    private final ReturnService returnService;

    /**
     * GET /api/admin/returns
     * List all return requests (optionally filter by status).
     */
    @GetMapping
    public ResponseEntity<List<ReturnResponse>> getAllReturns(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(returnService.getReturnsByStatus(status));
        }
        return ResponseEntity.ok(returnService.getAllReturns());
    }

    /**
     * GET /api/admin/returns/{returnId}
     * Get a specific return by ID.
     */
    @GetMapping("/{returnId}")
    public ResponseEntity<ReturnResponse> getReturn(@PathVariable Long returnId) {
        // Admin can see any return — reuse getReturnById with a sentinel customerId
        return ResponseEntity.ok(returnService.getAllReturns().stream()
                .filter(r -> r.getReturnId().equals(returnId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Return not found: " + returnId)));
    }

    /**
     * PATCH /api/admin/returns/{returnId}/decision
     * Approve or reject a pending return.
     * Body: { "decision": "APPROVED" | "REJECTED", "adminNote": "...", "rejectionReason": "...", "refundAmount": 0.0 }
     */
    @PatchMapping("/{returnId}/decision")
    public ResponseEntity<ReturnResponse> processDecision(
            @PathVariable Long returnId,
            @Valid @RequestBody AdminDecisionRequest decisionRequest) {
        return ResponseEntity.ok(returnService.processAdminDecision(returnId, decisionRequest));
    }

    @PostMapping("/{returnId}/refund")
    public ResponseEntity<ReturnResponse> processRefund(@PathVariable Long returnId) {
        return ResponseEntity.ok(returnService.processManualRefund(returnId));
    }

    /**
     * GET /api/admin/returns/order/{orderId}
     * Get all returns for a specific order.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<ReturnResponse>> getReturnsByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(returnService.getAllReturns().stream()
                .filter(r -> r.getOrderId().equals(orderId))
                .toList());
    }
}
