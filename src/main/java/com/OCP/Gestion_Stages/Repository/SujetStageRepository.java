package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.SujetStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SujetStageRepository extends JpaRepository<SujetStage, Long> {
    List<SujetStage> findByEncadrantIdOrderByCreatedAtDesc(Long encadrantId);
    List<SujetStage> findByStatutOrderByCreatedAtDesc(String statut);
    List<SujetStage> findAllByOrderByCreatedAtDesc();
    List<SujetStage> findByStageId(Long stageId);
}