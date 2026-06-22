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
        kafkaTemplate.send(USER_EVENTS_TOPIC, event);

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
        dto.setActive(entity.isActive());
        dto.setGender(entity.getGender());
        dto.setContactNum(entity.getContactNum());
        dto.setProfilePicURL(entity.getProfilePicURL());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public void forgotPassword(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

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
        kafkaTemplate.send("password-reset-events", event);
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
        user.setActive(dto.isActive());
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
