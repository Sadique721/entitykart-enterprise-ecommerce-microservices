package com.entitykart.productservice.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryUpdatedEvent {

    private Long categoryId;
    private String categoryName;
    private Boolean active;
    private LocalDateTime timestamp;
}
