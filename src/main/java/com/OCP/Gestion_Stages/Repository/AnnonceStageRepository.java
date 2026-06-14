package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.AnnonceStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnnonceStageRepository extends JpaRepository<AnnonceStage, Long> {
    List<AnnonceStage> findByActifTrueOrderByCreatedAtDesc();
    List<AnnonceStage> findAllByOrderByCreatedAtDesc();
}