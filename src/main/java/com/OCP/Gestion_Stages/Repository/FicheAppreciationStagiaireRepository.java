package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.FicheAppreciationStagiaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FicheAppreciationStagiaireRepository extends JpaRepository<FicheAppreciationStagiaire, Long> {
    Optional<FicheAppreciationStagiaire> findByStageId(Long stageId);
    boolean existsByStageId(Long stageId);
    List<FicheAppreciationStagiaire> findByEncadrantId(Long encadrantId);
}
