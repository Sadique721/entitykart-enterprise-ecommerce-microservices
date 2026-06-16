package com.entitykart.notificationservice.repository;

import com.entitykart.notificationservice.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findByUserId(Long userId);
    List<NotificationEntity> findByEmail(String email);
    List<NotificationEntity> findByStatus(NotificationEntity.NotificationStatus status);
    List<NotificationEntity> findByType(NotificationEntity.NotificationType type);
}
