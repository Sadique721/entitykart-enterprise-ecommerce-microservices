package com.entitykart.productservice.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CategoryDTO {

    private Long categoryId;
    private String categoryName;
    private Boolean active;
    private LocalDateTime createdAt;
}
