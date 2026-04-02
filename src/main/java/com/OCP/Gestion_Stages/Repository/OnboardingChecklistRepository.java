package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.OnboardingChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OnboardingChecklistRepository extends JpaRepository<OnboardingChecklist, Long> {
    List<OnboardingChecklist> findByStagiaireIdOrderByOrdreAsc(Long stagiaireId);
    long countByStagiaireIdAndCompletedTrue(Long stagiaireId);
    long countByStagiaireId(Long stagiaireId);
}