package com.entitykart.userservice.controller;

import com.entitykart.userservice.dto.UserDTO;
import com.entitykart.userservice.dto.LoginRequest;
import com.entitykart.userservice.dto.LoginResponse;
import com.entitykart.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public UserDTO register(@Valid @RequestBody UserDTO userDTO) {
        return userService.register(userDTO);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        return userService.login(loginRequest);
    }

    @PostMapping("/forgot-password")
    public void forgotPassword(@RequestParam String email) {
        userService.forgotPassword(email);
    }

    @PostMapping("/reset-password")
    public void resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        userService.resetPassword(token, newPassword);
    }

    @GetMapping("/all")
    public List<UserDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    /**
     * Used by order-service FeignClient (UserServiceClient) to fetch user info for order enrichment.
     */
    @GetMapping("/{id}")
    public UserDTO getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    public UserDTO updateUser(@PathVariable Long id,
                              @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInUserId,
                              @RequestHeader(value = "X-User-Role", required = false) String loggedInUserRole,
                              @RequestBody UserDTO userDTO) {
        if (loggedInUserId != null && !id.equals(loggedInUserId) && !"ADMIN".equalsIgnoreCase(loggedInUserRole)) {
            throw new RuntimeException("Unauthorized to update this profile");
        }
        return userService.updateUser(id, userDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id,
                           @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInUserId,
                           @RequestHeader(value = "X-User-Role", required = false) String loggedInUserRole) {
        if (loggedInUserId != null && !id.equals(loggedInUserId) && !"ADMIN".equalsIgnoreCase(loggedInUserRole)) {
            throw new RuntimeException("Unauthorized to delete this account");
        }
        userService.deleteUser(id);
    }

    @PostMapping("/deactivate")
    public void deactivateAccount(@RequestHeader("X-Customer-Id") Long userId) {
        userService.deleteUser(userId);
    }

    @PatchMapping("/{id}/toggle-status")
    public UserDTO toggleUserStatus(@PathVariable Long id) {
        return userService.toggleUserStatus(id);
    }

    @GetMapping("/stats")
    public java.util.Map<String, Object> getUserStats() {
        return userService.getUserStats();
    }
}
