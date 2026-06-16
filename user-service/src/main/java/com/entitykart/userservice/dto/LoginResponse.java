package com.entitykart.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String name;
    private String email;
    private String role;
    private long expiresIn; // ms

    public LoginResponse(String token, Long userId, String name, String email, String role, long expiresIn) {
        this.token = token;
        this.tokenType = "Bearer";
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.expiresIn = expiresIn;
    }
}
