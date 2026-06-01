package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.CompteStagiaireLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompteStagiaireLogRepository extends JpaRepository<CompteStagiaireLog, Long> {
    List<CompteStagiaireLog> findByStagiaireIdOrderByDateActionDesc(Long stagiaireId);
}
