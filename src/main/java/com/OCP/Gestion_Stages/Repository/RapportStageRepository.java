package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.RapportStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RapportStageRepository extends JpaRepository<RapportStage, Long> {
    Optional<RapportStage> findByStageId(Long stageId);

    @Query("SELECT r.id, r.nomFichier, r.typeContenu, r.taille, r.uploadedAt, r.stage.id FROM RapportStage r WHERE r.stage.id = :stageId")
    Optional<RapportStage> findMetaByStageId(Long stageId);

    List<RapportStage> findByStageEncadrantId(Long encadrantId);
}