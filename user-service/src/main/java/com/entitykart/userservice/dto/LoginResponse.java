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
    private String profilePicURL;
    private long expiresIn; // ms
    private String refreshToken; // opaque refresh token (null if rememberMe = false)

    /** Backward-compatible constructor (no refresh token) */
    public LoginResponse(String token, Long userId, String name, String email,
                         String role, String profilePicURL, long expiresIn) {
        this.token = token;
        this.tokenType = "Bearer";
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.profilePicURL = profilePicURL;
        this.expiresIn = expiresIn;
        this.refreshToken = null;
    }

    /** Full constructor including refresh token */
    public LoginResponse(String token, Long userId, String name, String email,
                         String role, String profilePicURL, long expiresIn, String refreshToken) {
        this(token, userId, name, email, role, profilePicURL, expiresIn);
        this.refreshToken = refreshToken;
    }
}
