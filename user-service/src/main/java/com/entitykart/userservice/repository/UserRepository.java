package com.entitykart.userservice.repository;

import com.entitykart.userservice.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmail(String email);
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByResetToken(String resetToken);
}

