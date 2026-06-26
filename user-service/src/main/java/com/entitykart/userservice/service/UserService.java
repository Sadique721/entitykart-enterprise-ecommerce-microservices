package com.entitykart.userservice.service;

import com.entitykart.userservice.dto.UserDTO;
import com.entitykart.userservice.dto.LoginRequest;
import com.entitykart.userservice.dto.LoginResponse;
import com.entitykart.userservice.entity.UserEntity;
import com.entitykart.userservice.event.UserCreatedEvent;
import com.entitykart.userservice.event.PasswordResetEvent;
import com.entitykart.userservice.repository.UserRepository;
import com.entitykart.userservice.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final String USER_EVENTS_TOPIC = "user-events";

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    public UserDTO register(UserDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("User already exists with email: " + dto.getEmail());
        }

        UserEntity user = new UserEntity();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password is required");
        }
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("USER");
        user.setActive(true);
        user.setGender(dto.getGender());
        user.setContactNum(dto.getContactNum());
        user.setProfilePicURL(dto.getProfilePicURL());

        UserEntity saved = userRepository.save(user);

        UserCreatedEvent event = new UserCreatedEvent(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getRole());
        try {
            kafkaTemplate.send(USER_EVENTS_TOPIC, event);
            log.info("UserCreatedEvent published for userId={}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to publish UserCreatedEvent for userId={}: {}", saved.getId(), e.getMessage());
            // Do NOT rethrow — user is already saved in DB, registration succeeded
        }

        return convertToDTO(saved);
    }

    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!user.isActive()) {
            throw new RuntimeException("User account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        Key key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        String token = io.jsonwebtoken.Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        return new LoginResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole(), jwtExpiration);
    }

    private UserDTO convertToDTO(UserEntity entity) {
        UserDTO dto = new UserDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setEmail(entity.getEmail());
        dto.setRole(entity.getRole());
        dto.setActive(entity.isActive());  // Convert primitive to Boolean
        dto.setGender(entity.getGender());
        dto.setContactNum(entity.getContactNum());
        dto.setProfilePicURL(entity.getProfilePicURL());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    /**
     * Forgot-password: saves a reset token and publishes Kafka event so
     * notification-service emails it to the user.
     *
     * Design: NEVER throw an exception to the caller — always return void (200 OK).
     * This prevents email enumeration attacks AND prevents "Failed to send token"
     * errors when Kafka is momentarily slow or the user doesn't exist.
     */
    public void forgotPassword(String email) {
        // Security: silently ignore if email not registered
        var optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            log.warn("Forgot-password request for unregistered email: {}", email);
            return;  // Return 200 OK — do not reveal whether email exists
        }

        UserEntity user = optUser.get();
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        PasswordResetEvent event = new PasswordResetEvent(
                user.getId(),
                user.getName(),
                user.getEmail(),
                token
        );

        // Kafka send: wrap in try-catch so HTTP response is always 200
        // Email may be delayed if Kafka is starting up, but will be delivered
        try {
            kafkaTemplate.send("password-reset-events", event);
            log.info("Password reset event published for userId={}, email={}", user.getId(), email);
        } catch (Exception e) {
            log.error("Failed to publish password-reset event for {}: {}", email, e.getMessage());
            // Do NOT rethrow — token is saved in DB, user can retry
        }
    }

    public void resetPassword(String token, String newPassword) {
        UserEntity user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Fetch a single user by ID — called by order-service via FeignClient.
     */
    public UserDTO getUserById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return convertToDTO(user);
    }

    public UserDTO updateUser(Long id, UserDTO dto) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        // Preserve active status: only change if explicitly provided in payload
        // Boolean null means field was not sent → keep existing active value
        if (dto.getActive() != null) {
            user.setActive(dto.getActive());
        }
        // If null → keep user.active unchanged (prevents accidental deactivation)
        user.setGender(dto.getGender());
        user.setContactNum(dto.getContactNum());
        user.setProfilePicURL(dto.getProfilePicURL());

        UserEntity saved = userRepository.save(user);
        return convertToDTO(saved);
    }

    public void deleteUser(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setActive(false);
        userRepository.save(user);
    }

    public UserDTO toggleUserStatus(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setActive(!user.isActive());
        UserEntity saved = userRepository.save(user);
        return convertToDTO(saved);
    }

    public java.util.Map<String, Object> getUserStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        // stats.put("totalUsers", userRepository.count());
        stats.put("totalUsers", userRepository.count());
        stats.put("totalAdmins", userRepository.countByRole("ADMIN"));
        stats.put("totalActive", userRepository.countByActive(true));
        stats.put("totalSellers", userRepository.countByRole("SELLER"));
        stats.put("totalCities", addressRepository.countDistinctCities());
        return stats;
    }
}
