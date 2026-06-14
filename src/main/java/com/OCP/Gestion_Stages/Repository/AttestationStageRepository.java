package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.AttestationStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttestationStageRepository extends JpaRepository<AttestationStage, Long> {
    List<AttestationStage> findAllByOrderByDateDemandeDesc();
    Optional<AttestationStage> findByStageId(Long stageId);
    List<AttestationStage> findByStatutOrderByDateDemandeDesc(String statut);
}