package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.Candidature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, Long> {
    List<Candidature> findByStatutOrderByCreatedAtDesc(String statut);
    List<Candidature> findAllByOrderByCreatedAtDesc();
    boolean existsByEmail(String email);
    @Query("SELECT COALESCE(AVG(c.scoreMatching), 0) FROM Candidature c WHERE c.scoreMatching > 0")
    double findAverageScoreMatching();

    long countByStatut(String statut);
    List<Candidature> findByDepartementId(Long departementId);
    List<Candidature> findByDepartementIdAndStatutNot(Long departementId, String statut);
    List<Candidature> findByDepartementIdOrderByCreatedAtDesc(Long departementId);
    long countByDepartementIdAndStatut(Long departementId, String statut);

    List<Candidature> findByDepartementIdAndStatutInOrderByScoreMatchingDesc(Long departementId, List<String> statuts);

    // Migration bytea → disque : CV encore stockés en base
    @Query("SELECT c.id FROM Candidature c WHERE c.cvChemin IS NULL AND c.cvContenu IS NOT NULL")
    List<Long> findCvIdsAMigrer();
}