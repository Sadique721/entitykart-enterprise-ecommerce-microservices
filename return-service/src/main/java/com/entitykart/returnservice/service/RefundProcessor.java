package com.entitykart.returnservice.service;

import com.entitykart.returnservice.client.PaymentServiceClient;
import com.entitykart.returnservice.entity.ReturnEntity;
import com.entitykart.returnservice.repository.ReturnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundProcessor {

    private final PaymentServiceClient paymentServiceClient;
    private final ReturnRepository returnRepository;

    /**
     * Processes an approved return refund via payment-service.
     * Runs in a new transaction so the parent APPROVED status is persisted first.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processRefund(ReturnEntity returnEntity) {
        log.info("Processing refund for returnId={}, amount={}", 
                 returnEntity.getReturnId(), returnEntity.getRefundAmount());
        try {
            paymentServiceClient.processRefund(
                    returnEntity.getOrderId(),
                    returnEntity.getRefundAmount(),
                    "REFUND"
            );
            returnEntity.setStatus(ReturnEntity.ReturnStatus.REFUNDED);
            returnRepository.save(returnEntity);
            log.info("Refund SUCCESS for returnId={}, orderId={}", 
                     returnEntity.getReturnId(), returnEntity.getOrderId());
        } catch (Exception e) {
            // Keep status as APPROVED; admin can retry later
            log.error("Refund FAILED for returnId={}: {}. Status remains APPROVED for manual retry.",
                      returnEntity.getReturnId(), e.getMessage());
        }
    }
}
