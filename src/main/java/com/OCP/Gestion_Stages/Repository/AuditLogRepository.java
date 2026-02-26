package com.OCP.Gestion_Stages.Repository;



import com.OCP.Gestion_Stages.domain.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUtilisateur(String utilisateur);
    List<AuditLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    List<AuditLog> findByAction(String action);
}