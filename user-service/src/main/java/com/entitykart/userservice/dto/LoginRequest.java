package com.entitykart.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    /** When true, a long-lived refresh token (90 days) is issued alongside the JWT. */
    private Boolean rememberMe = false;
}
