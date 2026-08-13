package com.entitykart.paymentservice.controller;

import com.entitykart.paymentservice.client.OrderServiceClient;
import com.entitykart.paymentservice.dto.EmiPaymentRequest;
import com.entitykart.paymentservice.dto.OrderDTO;
import com.entitykart.paymentservice.dto.PaymentDTO;
import com.entitykart.paymentservice.dto.PaymentRequest;
import com.entitykart.paymentservice.entity.PaymentEntity;
import com.entitykart.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderServiceClient orderServiceClient;

    // ─── Card (Authorize.Net sandbox or mock) ──────────────────────────────────
    @PostMapping("/process-card")
    public PaymentEntity processCardPayment(@RequestBody PaymentRequest request) {
        return paymentService.processCardPayment(request);
    }

    // ─── UPI / COD generic offline ─────────────────────────────────────────────
    @PostMapping("/process-offline")
    public PaymentEntity processOfflinePayment(
            @RequestParam Long orderId,
            @RequestParam Double amount,
            @RequestParam String paymentMode,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String customerName) {
        return paymentService.processOfflinePayment(orderId, amount, paymentMode, customerEmail, customerName);
    }

    // ─── Net Banking ───────────────────────────────────────────────────────────
    @PostMapping("/process-netbanking")
    public PaymentEntity processNetBankingPayment(
            @RequestParam Long orderId,
            @RequestParam Double amount,
            @RequestParam(required = false, defaultValue = "SBI") String bankName,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String customerName) {
        return paymentService.processNetBankingPayment(orderId, amount, bankName, customerEmail, customerName);
    }

    // ─── Wallet (Paytm / PhonePe / Amazon Pay / MobiKwik) ────────────────────
    @PostMapping("/process-wallet")
    public PaymentEntity processWalletPayment(
            @RequestParam Long orderId,
            @RequestParam Double amount,
            @RequestParam(required = false, defaultValue = "PAYTM") String walletType,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String customerName) {
        return paymentService.processWalletPayment(orderId, amount, walletType, customerEmail, customerName);
    }

    // ─── EMI ──────────────────────────────────────────────────────────────────
    @PostMapping("/process-emi")
    public PaymentEntity processEmiPayment(@RequestBody EmiPaymentRequest req) {
        // CRIT-3 FIX: Card data received in POST body only — never as URL params
        return paymentService.processEmiPayment(
                req.getOrderId(), req.getAmount(), req.getCardNumber(),
                req.getEmiTenure() != null ? req.getEmiTenure() : 3,
                req.getCustomerEmail(), req.getCustomerName());
    }


    // ─── Assign COD transaction when order is DELIVERED ──────────────────────
    @PostMapping("/assign-cod-transaction/{orderId}")
    public PaymentEntity assignCodTransaction(@PathVariable Long orderId) {
        return paymentService.assignCodTransaction(orderId);
    }

    @GetMapping("/order/{orderId}")
    public org.springframework.http.ResponseEntity<PaymentDTO> getPaymentByOrder(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Customer-Id", required = false) Long requestingCustomerId,
            @RequestHeader(value = "X-User-Role", required = false) String requestingRole) {

        if (!"ADMIN".equalsIgnoreCase(requestingRole)) {
            try {
                OrderDTO order = orderServiceClient.getOrder(orderId);
                if (order == null || requestingCustomerId == null
                        || !requestingCustomerId.equals(order.getCustomerId())) {
                    log.warn("Blocked payment lookup for orderId={} — requester customerId={} does not own it",
                            orderId, requestingCustomerId);
                    return org.springframework.http.ResponseEntity.status(403).build();
                }
            } catch (Exception e) {
                log.error("Could not verify order ownership for orderId={}: {}", orderId, e.getMessage());
                return org.springframework.http.ResponseEntity.status(502).build();
            }
        }

        try {
            PaymentEntity entity = paymentService.getPaymentByOrderId(orderId);
            return org.springframework.http.ResponseEntity.ok(toDTO(entity));
        } catch (Exception e) {
            log.info("No payment record yet for orderId={}: {}", orderId, e.getMessage());
            return org.springframework.http.ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/all")
    public List<PaymentDTO> getAllPayments() {
        return paymentService.getAllPayments().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ─── Internal mapper ──────────────────────────────────────────────────────
    private PaymentDTO toDTO(PaymentEntity entity) {
        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId(entity.getPaymentId());
        dto.setOrderId(entity.getOrderId());
        dto.setAmount(entity.getAmount());
        dto.setPaymentMode(entity.getPaymentMode() != null ? entity.getPaymentMode().name() : "");
        dto.setTransactionRef(entity.getTransactionRef());
        dto.setPaymentStatus(entity.getPaymentStatus() != null ? entity.getPaymentStatus().name() : "");
        dto.setPaymentDate(entity.getPaymentDate());
        return dto;
    }
}
