package com.entitykart.userservice.service;

import com.entitykart.shared.util.HashUtils;
import com.entitykart.userservice.dto.UserSessionDTO;
import com.entitykart.userservice.entity.RefreshTokenEntity;
import com.entitykart.userservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public String createRefreshToken(Long userId, String userAgent, String ipAddress, String deviceFingerprint, boolean rememberMe) {
        String rawToken = UUID.randomUUID().toString();
        String hashedToken = HashUtils.sha256(rawToken);

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(userId);
        entity.setTokenHash(hashedToken);
        entity.setUserAgent(userAgent);
        entity.setIpAddress(ipAddress);
        entity.setDeviceFingerprint(deviceFingerprint);
        
        long expiryDays = rememberMe ? 90 : 30;
        entity.setExpiresAt(LocalDateTime.now().plusDays(expiryDays));
        entity.setRevoked(false);

        refreshTokenRepository.save(entity);
        log.info("Created refresh token session for userId={}, rememberMe={}", userId, rememberMe);
        return rawToken;
    }

    @Transactional
    public RefreshTokenEntity validateAndGet(String rawToken) {
        String hashedToken = HashUtils.sha256(rawToken);
        RefreshTokenEntity entity = refreshTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (entity.isRevoked()) {
            log.warn("Revoked refresh token presented! Revoking all sessions for userId={} due to suspected token theft.", entity.getUserId());
            refreshTokenRepository.deleteByUserId(entity.getUserId());
            throw new RuntimeException("Session has been compromised and terminated");
        }

        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(entity);
            throw new RuntimeException("Session has expired");
        }

        return entity;
    }

    @Transactional
    public void rotateToken(String oldRawToken, String newRawToken) {
        String oldHash = HashUtils.sha256(oldRawToken);
        String newHash = HashUtils.sha256(newRawToken);

        RefreshTokenEntity entity = refreshTokenRepository.findByTokenHash(oldHash)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        entity.setRevoked(true);
        entity.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(entity);

        RefreshTokenEntity newEntity = new RefreshTokenEntity();
        newEntity.setUserId(entity.getUserId());
        newEntity.setTokenHash(newHash);
        newEntity.setUserAgent(entity.getUserAgent());
        newEntity.setIpAddress(entity.getIpAddress());
        newEntity.setDeviceFingerprint(entity.getDeviceFingerprint());
        newEntity.setExpiresAt(LocalDateTime.now().plusDays(30));
        newEntity.setRevoked(false);
        refreshTokenRepository.save(newEntity);
    }

    @Transactional
    public void revokeSession(String rawToken) {
        String hashed = HashUtils.sha256(rawToken);
        refreshTokenRepository.findByTokenHash(hashed).ifPresent(entity -> {
            entity.setRevoked(true);
            entity.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(entity);
            log.info("Revoked session ID={} for userId={}", entity.getId(), entity.getUserId());
        });
    }

    @Transactional
    public void revokeSessionById(Long sessionId, Long userId) {
        refreshTokenRepository.findById(sessionId).ifPresent(entity -> {
            if (entity.getUserId().equals(userId)) {
                refreshTokenRepository.delete(entity);
                log.info("Revoked specific session ID={} for userId={}", sessionId, userId);
            } else {
                throw new RuntimeException("Unauthorized to revoke this session");
            }
        });
    }

    @Transactional
    public void revokeAllUserSessions(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("Revoked all active sessions for userId={}", userId);
    }

    /** Alias used by UserController for the global-logout endpoint. */
    @Transactional
    public void revokeAllSessionsForUser(Long userId) {
        revokeAllUserSessions(userId);
    }

    @Transactional(readOnly = true)
    public List<UserSessionDTO> getActiveSessions(Long userId) {
        return refreshTokenRepository.findByUserIdAndRevoked(userId, false).stream()
                .map(entity -> new UserSessionDTO(
                        entity.getId(),
                        entity.getDeviceFingerprint(),
                        entity.getIpAddress(),
                        entity.getUserAgent(),
                        entity.getCreatedAt(),
                        entity.getExpiresAt()
                ))
                .collect(Collectors.toList());
    }
}
