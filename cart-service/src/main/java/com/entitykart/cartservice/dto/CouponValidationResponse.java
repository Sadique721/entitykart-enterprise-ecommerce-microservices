package com.entitykart.cartservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for coupon/promo code validation.
 * MED-5: Coupon validation now handled server-side.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResponse {
    private boolean valid;
    private String code;
    private String type;           // PERCENT | FIXED
    private Double discountValue;  // percent (0-100) or fixed rupee amount
    private Double maxDiscount;    // cap for PERCENT coupons (null = no cap)
    private String message;
}
