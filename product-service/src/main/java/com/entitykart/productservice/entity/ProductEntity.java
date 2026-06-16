package com.entitykart.productservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "products")
@Data
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private LocalDateTime createdAt;
    private String status = "Available";

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "Available";
        }
    }

    public BigDecimal getDiscountPercent() {
        if (mrp != null && mrp.compareTo(BigDecimal.ZERO) > 0 && price != null) {
            return mrp.subtract(price)
                    .divide(mrp, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }
}
