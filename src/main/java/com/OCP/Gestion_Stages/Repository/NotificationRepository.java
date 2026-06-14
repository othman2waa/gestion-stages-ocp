package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndLueFalseOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndLueFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.lue = true WHERE n.user.id = :userId AND n.lue = false")
    void markAllAsRead(Long userId);
}
