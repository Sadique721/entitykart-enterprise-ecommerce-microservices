package com.entitykart.commonservices.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Kafka event published by user-service on forgot-password request. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetEvent {
    private Long id;
    private String name;
    private String email;
    private String token;
}
