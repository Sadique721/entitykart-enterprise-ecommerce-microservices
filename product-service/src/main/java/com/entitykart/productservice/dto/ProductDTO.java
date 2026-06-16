package com.entitykart.productservice.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class ProductDTO {

    private Long productId;
    private String productName;
    private String description;
    private String brand;
    private BigDecimal price;
    private BigDecimal mrp;
    private Integer stockQuantity;
    private String sku;
    private String mainImageURL;
    private Long categoryId;
    private Long subCategoryId;
    private Long sellerId;
    private BigDecimal discountPercent;
}
