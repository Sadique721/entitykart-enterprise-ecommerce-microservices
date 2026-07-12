package com.entitykart.userservice;

import com.entitykart.shared.dto.UserCreatedEvent;
import com.entitykart.userservice.dto.LoginRequest;
import com.entitykart.userservice.dto.LoginResponse;
import com.entitykart.userservice.dto.UserDTO;
import com.entitykart.userservice.dto.UserSessionDTO;
import com.entitykart.userservice.entity.RefreshTokenEntity;
import com.entitykart.userservice.entity.UserEntity;
import com.entitykart.userservice.repository.RefreshTokenRepository;
import com.entitykart.userservice.repository.UserRepository;
import com.entitykart.userservice.service.JwtService;
import com.entitykart.userservice.service.RefreshTokenService;
import com.entitykart.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    private UserEntity testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    public void setup() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setRole("USER");
        testUser.setActive(true);

        testUserDTO = new UserDTO();
        testUserDTO.setName("John Doe");
        testUserDTO.setEmail("john@example.com");
        testUserDTO.setPassword("ValidPass123!");
    }

    // --- REGISTRATION TESTS (5 Tests) ---

    @Test
    public void testRegisterUser_Success() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("ValidPass123!")).thenReturn("hashedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        UserDTO registered = userService.register(testUserDTO);

        assertNotNull(registered);
        assertEquals("john@example.com", registered.getEmail());
        verify(userRepository, times(1)).save(any(UserEntity.class));
        verify(kafkaTemplate, times(1)).send(eq("user-events"), any(UserCreatedEvent.class));
    }

    @Test
    public void testRegisterUser_DuplicateEmail() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.register(testUserDTO));
    }

    @Test
    public void testRegisterUser_PasswordTooShort() {
        testUserDTO.setPassword("short");
        assertThrows(RuntimeException.class, () -> userService.register(testUserDTO));
    }

    @Test
    public void testRegisterUser_PasswordNoUppercase() {
        testUserDTO.setPassword("nouppercase123!");
        assertThrows(RuntimeException.class, () -> userService.register(testUserDTO));
    }

    @Test
    public void testRegisterUser_PasswordNoSpecialChar() {
        testUserDTO.setPassword("NoSpecialChar123");
        assertThrows(RuntimeException.class, () -> userService.register(testUserDTO));
    }

    // --- AUTHENTICATION & LOCKOUT TESTS (5 Tests) ---

    @Test
    public void testLogin_Success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("john@example.com");
        req.setPassword("password");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("jwtToken");

        LoginResponse response = userService.login(req);

        assertNotNull(response);
        assertEquals("jwtToken", response.getToken());
    }

    @Test
    public void testLogin_IncorrectPassword_ThrowsException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("john@example.com");
        req.setPassword("wrong");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "hashedPassword")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> userService.login(req));
    }

    @Test
    public void testLogin_BruteForceLockout_TriggersAfter5Attempts() {
        LoginRequest req = new LoginRequest();
        req.setEmail("john@example.com");
        req.setPassword("wrong");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "hashedPassword")).thenReturn(false);

        // Fail 5 times to trigger lockout
        for (int i = 0; i < 4; i++) {
            assertThrows(RuntimeException.class, () -> userService.login(req));
        }

        // 5th failure should trigger warning lock message
        Exception ex = assertThrows(RuntimeException.class, () -> userService.login(req));
        assertNotNull(ex.getMessage());
    }

    @Test
    public void testLogin_LockedAccount_FailsImmediately() {
        LoginRequest req = new LoginRequest();
        req.setEmail("john@example.com");
        req.setPassword("password");
        // Force the brute force in-memory lock
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "hashedPassword")).thenReturn(false);

        for (int i = 0; i < 5; i++) {
            assertThrows(RuntimeException.class, () -> userService.login(req));
        }

        // Try with correct password - should fail due to lock
        assertThrows(RuntimeException.class, () -> userService.login(req));
    }

    @Test
    public void testLogin_InactiveUser_Fails() {
        testUser.setActive(false);
        LoginRequest req = new LoginRequest();
        req.setEmail("john@example.com");
        req.setPassword("password");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(RuntimeException.class, () -> userService.login(req));
    }

    // --- FORGOT / RESET PASSWORD TESTS (5 Tests) ---

    @Test
    public void testForgotPassword_Success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        assertDoesNotThrow(() -> userService.forgotPassword("john@example.com"));
        assertNotNull(testUser.getResetToken());
        assertNotNull(testUser.getResetTokenExpiry());
        verify(kafkaTemplate, times(1)).send(eq("password-reset-events"), any());
    }

    @Test
    public void testForgotPassword_NonExistentEmail() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> userService.forgotPassword("missing@example.com"));
        verify(userRepository, never()).save(any());
    }

    @Test
    public void testResetPassword_Success() {
        testUser.setResetToken("hashedToken");
        testUser.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        when(userRepository.findByResetToken(any())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewSecurePassword123!")).thenReturn("newHashedPass");

        userService.resetPassword("rawToken", "NewSecurePassword123!");

        assertEquals("newHashedPass", testUser.getPassword());
        assertNull(testUser.getResetToken());
        assertNull(testUser.getResetTokenExpiry());
    }

    @Test
    public void testResetPassword_ExpiredToken() {
        testUser.setResetToken("hashedToken");
        testUser.setResetTokenExpiry(LocalDateTime.now().minusMinutes(5));
        when(userRepository.findByResetToken(any())).thenReturn(Optional.of(testUser));

        assertThrows(RuntimeException.class, () -> userService.resetPassword("rawToken", "NewSecurePassword123!"));
    }

    @Test
    public void testResetPassword_InvalidToken() {
        when(userRepository.findByResetToken(any())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.resetPassword("invalidToken", "NewSecurePassword123!"));
    }

    // --- REFRESH TOKEN SERVICE TESTS (5 Tests) ---

    @Test
    public void testRefreshTokenService_CreateToken() {
        RefreshTokenService tokenService = new RefreshTokenService(refreshTokenRepository);
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        String rawToken = tokenService.createRefreshToken(1L, "Agent", "127.0.0.1", "Fingerprint", true);

        assertNotNull(rawToken);
        verify(refreshTokenRepository, times(1)).save(any());
    }

    @Test
    public void testRefreshTokenService_ValidateAndGet_Success() {
        RefreshTokenService tokenService = new RefreshTokenService(refreshTokenRepository);
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(1L);
        entity.setExpiresAt(LocalDateTime.now().plusDays(10));
        entity.setRevoked(false);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(entity));

        RefreshTokenEntity validated = tokenService.validateAndGet("someRawToken");

        assertNotNull(validated);
        assertEquals(1L, validated.getUserId());
    }

    @Test
    public void testRefreshTokenService_ValidateAndGet_CompromisedToken() {
        RefreshTokenService tokenService = new RefreshTokenService(refreshTokenRepository);
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(1L);
        entity.setRevoked(true);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(entity));

        assertThrows(RuntimeException.class, () -> tokenService.validateAndGet("compromisedToken"));
        verify(refreshTokenRepository, times(1)).deleteByUserId(1L);
    }

    @Test
    public void testRefreshTokenService_RevokeSession() {
        RefreshTokenService tokenService = new RefreshTokenService(refreshTokenRepository);
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(1L);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(entity));
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        assertDoesNotThrow(() -> tokenService.revokeSession("token"));
        assertTrue(entity.isRevoked());
    }

    @Test
    public void testRefreshTokenService_GetActiveSessions() {
        RefreshTokenService tokenService = new RefreshTokenService(refreshTokenRepository);
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(100L);
        entity.setDeviceFingerprint("fingerprint");
        entity.setIpAddress("127.0.0.1");
        entity.setUserAgent("Agent");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByUserIdAndRevoked(1L, false)).thenReturn(List.of(entity));

        List<UserSessionDTO> list = tokenService.getActiveSessions(1L);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("fingerprint", list.get(0).getDeviceFingerprint());
    }
}
