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

    @Query("SELECT s FROM Stage s WHERE s.encadrant.id = :encadrantId " +
            "AND s.statut = :statut")
    List<Stage> findByEncadrantIdAndStatut(Long encadrantId, StageStatus statut);
    @Query("""
    SELECT s FROM Stage s
    LEFT JOIN s.stagiaire st
    LEFT JOIN s.encadrant e
    LEFT JOIN s.departement d
    WHERE (:keyword IS NULL OR LOWER(s.sujet) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(st.nom) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(st.prenom) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND (:statut IS NULL OR s.statut = :statut)
    AND (:typeStage IS NULL OR s.typeStage = :typeStage)
    AND (:departementId IS NULL OR d.id = :departementId)
    """)
    Page<Stage> rechercher(
            @Param("keyword") String keyword,
            @Param("statut") StageStatus statut,
            @Param("typeStage") TypeStage typeStage,
            @Param("departementId") Long departementId,
            Pageable pageable
    );

}