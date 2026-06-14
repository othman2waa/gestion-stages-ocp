package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.SuiviHebdomadaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SuiviHebdomadaireRepository extends JpaRepository<SuiviHebdomadaire, Long> {
    List<SuiviHebdomadaire> findByStageIdOrderBySemaineNumeroAsc(Long stageId);
    List<SuiviHebdomadaire> findByEncadrantIdOrderByDateSuiviDesc(Long encadrantId);
}