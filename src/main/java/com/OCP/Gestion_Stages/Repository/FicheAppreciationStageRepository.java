package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.FicheAppreciationStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FicheAppreciationStageRepository extends JpaRepository<FicheAppreciationStage, Long> {
    Optional<FicheAppreciationStage> findByStageId(Long stageId);
    boolean existsByStageId(Long stageId);
    List<FicheAppreciationStage> findByEncadrantId(Long encadrantId);
}
