package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.StageHistorique;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StageHistoriqueRepository extends JpaRepository<StageHistorique, Long> {
    List<StageHistorique> findByStageIdOrderByDateModificationDesc(Long stageId);
}