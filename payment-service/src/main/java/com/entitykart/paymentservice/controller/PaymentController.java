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

    @PostMapping("/process-card")
    public PaymentEntity processCardPayment(@RequestBody PaymentRequest request) {
        return paymentService.processCardPayment(request);
    }

    @PostMapping("/process-offline")
    public PaymentEntity processOfflinePayment(
            @RequestParam Long orderId,
            @RequestParam Double amount,
            @RequestParam String paymentMode) {
        return paymentService.processOfflinePayment(orderId, amount, paymentMode);
    }

    @GetMapping("/order/{orderId}")
    public PaymentDTO getPaymentByOrder(@PathVariable Long orderId) {
        PaymentEntity entity = paymentService.getPaymentByOrderId(orderId);
        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId(entity.getPaymentId());
        dto.setOrderId(entity.getOrderId());
        dto.setAmount(entity.getAmount());
        dto.setPaymentMode(entity.getPaymentMode().name());
        dto.setTransactionRef(entity.getTransactionRef());
        dto.setPaymentStatus(entity.getPaymentStatus().name());
        dto.setPaymentDate(entity.getPaymentDate());
        return dto;
    }

    @GetMapping("/all")
    public List<PaymentDTO> getAllPayments() {
        return paymentService.getAllPayments().stream().map(entity -> {
            PaymentDTO dto = new PaymentDTO();
            dto.setPaymentId(entity.getPaymentId());
            dto.setOrderId(entity.getOrderId());
            dto.setAmount(entity.getAmount());
            dto.setPaymentMode(entity.getPaymentMode() != null ? entity.getPaymentMode().name() : "");
            dto.setTransactionRef(entity.getTransactionRef());
            dto.setPaymentStatus(entity.getPaymentStatus() != null ? entity.getPaymentStatus().name() : "");
            dto.setPaymentDate(entity.getPaymentDate());
            return dto;
        }).collect(Collectors.toList());
    }
}
