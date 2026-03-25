package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.Candidature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, Long> {
    List<Candidature> findByStatutOrderByCreatedAtDesc(String statut);
    List<Candidature> findAllByOrderByCreatedAtDesc();
    boolean existsByEmail(String email);
}