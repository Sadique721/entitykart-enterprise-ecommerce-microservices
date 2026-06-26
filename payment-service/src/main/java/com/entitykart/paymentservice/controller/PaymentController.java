package com.entitykart.paymentservice.controller;

import com.entitykart.paymentservice.dto.PaymentDTO;
import com.entitykart.paymentservice.dto.PaymentRequest;
import com.entitykart.paymentservice.entity.PaymentEntity;
import com.entitykart.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

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
    public PaymentEntity processEmiPayment(
            @RequestParam Long orderId,
            @RequestParam Double amount,
            @RequestParam(required = false) String cardNumber,
            @RequestParam(required = false, defaultValue = "3") Integer emiTenure,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String customerName) {
        return paymentService.processEmiPayment(orderId, amount, cardNumber, emiTenure, customerEmail, customerName);
    }

    // ─── Assign COD transaction when order is DELIVERED ──────────────────────
    @PostMapping("/assign-cod-transaction/{orderId}")
    public PaymentEntity assignCodTransaction(@PathVariable Long orderId) {
        return paymentService.assignCodTransaction(orderId);
    }

    // ─── Read endpoints ───────────────────────────────────────────────────────
    @GetMapping("/order/{orderId}")
    public PaymentDTO getPaymentByOrder(@PathVariable Long orderId) {
        PaymentEntity entity = paymentService.getPaymentByOrderId(orderId);
        return toDTO(entity);
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
