package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.Stage;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.enums.TypeStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface StageRepository extends JpaRepository<Stage, Long> {

    List<Stage> findByStagiaireId(Long stagiaireId);
    List<Stage> findByEncadrantId(Long encadrantId);
    List<Stage> findByDepartementId(Long departementId);
    List<Stage> findByStatut(StageStatus statut);
    List<Stage> findByTypeStage(TypeStage typeStage);
    List<Stage> findByDateFinAndStatut(LocalDate dateFin, StageStatus statut);

    @Query("SELECT s.departement.nom, COUNT(s) FROM Stage s GROUP BY s.departement.nom")
    List<Object[]> countByDepartement();

    @Query("SELECT s.typeStage, COUNT(s) FROM Stage s GROUP BY s.typeStage")
    List<Object[]> countByTypeStage();

    @Query("SELECT COUNT(s) FROM Stage s WHERE s.statut = :statut")
    Long countByStatut(StageStatus statut);

    @Query("SELECT s FROM Stage s WHERE s.encadrant.id = :encadrantId AND s.statut = :statut")
    List<Stage> findByEncadrantIdAndStatut(Long encadrantId, StageStatus statut);

    @Query(value = """
SELECT s.* FROM stage s
LEFT JOIN stagiaire st ON st.id = s.stagiaire_id
LEFT JOIN encadrant e ON e.id = s.encadrant_id
LEFT JOIN departement d ON d.id = s.departement_id
WHERE (CAST(:keyword AS VARCHAR) IS NULL
    OR LOWER(s.sujet) LIKE LOWER(CONCAT('%', CAST(:keyword AS VARCHAR), '%'))
    OR LOWER(st.nom) LIKE LOWER(CONCAT('%', CAST(:keyword AS VARCHAR), '%'))
    OR LOWER(st.prenom) LIKE LOWER(CONCAT('%', CAST(:keyword AS VARCHAR), '%'))
    OR LOWER(e.nom) LIKE LOWER(CONCAT('%', CAST(:keyword AS VARCHAR), '%'))
    OR LOWER(d.nom) LIKE LOWER(CONCAT('%', CAST(:keyword AS VARCHAR), '%')))
AND (CAST(:statut AS VARCHAR) IS NULL OR s.statut = CAST(:statut AS VARCHAR))
AND (CAST(:typeStage AS VARCHAR) IS NULL OR s.type_stage = CAST(:typeStage AS VARCHAR))
AND (CAST(:departementId AS BIGINT) IS NULL OR d.id = CAST(:departementId AS BIGINT))
""", nativeQuery = true)
    Page<Stage> rechercher(
            @Param("keyword") String keyword,
            @Param("statut") String statut,
            @Param("typeStage") String typeStage,
            @Param("departementId") Long departementId,
            Pageable pageable
    );
    long countByDepartementId(Long departementId);
    long countByDepartementIdAndStatut(Long departementId, StageStatus statut);
}