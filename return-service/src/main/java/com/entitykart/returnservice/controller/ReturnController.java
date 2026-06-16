package com.entitykart.returnservice.controller;

import com.entitykart.returnservice.dto.ReturnRequest;
import com.entitykart.returnservice.dto.ReturnResponse;
import com.entitykart.returnservice.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Customer-facing return endpoints.
 * customerId is passed as a request header (set by API Gateway after JWT validation).
 */
@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    /**
     * POST /api/returns
     * Create a new return request for a delivered order item.
     */
    @PostMapping
    public ResponseEntity<ReturnResponse> createReturn(
            @RequestHeader("X-Customer-Id") Long customerId,
            @Valid @RequestBody ReturnRequest request) {
        ReturnResponse response = returnService.createReturn(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/returns/my
     * Get all return requests for the authenticated customer.
     */
    @GetMapping("/my")
    public ResponseEntity<List<ReturnResponse>> getMyReturns(
            @RequestHeader("X-Customer-Id") Long customerId) {
        return ResponseEntity.ok(returnService.getReturnsByCustomer(customerId));
    }

    /**
     * GET /api/returns/{returnId}
     * Get a single return request (customer must own it).
     */
    @GetMapping("/{returnId}")
    public ResponseEntity<ReturnResponse> getReturnById(
            @PathVariable Long returnId,
            @RequestHeader("X-Customer-Id") Long customerId) {
        return ResponseEntity.ok(returnService.getReturnById(returnId, customerId));
    }
}
