package com.entitykart.commonservices.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Kafka event published by user-service when a new user registers. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent {
    private Long id;
    private String name;
    private String email;
    private String role;
}
