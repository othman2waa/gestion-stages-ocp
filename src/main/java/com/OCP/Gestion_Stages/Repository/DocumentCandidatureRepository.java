package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.DocumentCandidature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentCandidatureRepository extends JpaRepository<DocumentCandidature, Long> {
    List<DocumentCandidature> findByCandidatureId(Long candidatureId);
    void deleteByCandidatureId(Long candidatureId);
    long countByCandidatureId(Long candidatureId);

    // Migration bytea → disque : lignes encore stockées en base
    @Query("SELECT d.id FROM DocumentCandidature d WHERE d.cheminFichier IS NULL AND d.contenu IS NOT NULL")
    List<Long> findIdsAMigrer();
}