package com.entitykart.wishlistservice.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WishlistItemDTO {

    private Long wishlistId;
    private Long productId;
    private String productName;
    private String productImage;
    private Double price;
    private LocalDateTime addedAt;
}
