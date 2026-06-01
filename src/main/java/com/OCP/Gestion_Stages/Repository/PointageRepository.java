package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.Pointage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PointageRepository extends JpaRepository<Pointage, Long> {
    List<Pointage> findByStageIdOrderByDatePointageAsc(Long stageId);
    Optional<Pointage> findByStageIdAndDatePointage(Long stageId, LocalDate datePointage);
}
