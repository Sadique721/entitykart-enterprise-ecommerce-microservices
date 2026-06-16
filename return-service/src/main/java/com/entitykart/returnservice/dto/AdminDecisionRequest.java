package com.entitykart.returnservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminDecisionRequest {

    @NotBlank(message = "Decision is required: APPROVED or REJECTED")
    private String decision;

    private String adminNote;

    private String rejectionReason;

    // Optional: override refund amount. If null, computed automatically.
    private Double refundAmount;
}
