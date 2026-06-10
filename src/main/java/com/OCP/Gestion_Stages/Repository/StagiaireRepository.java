package com.OCP.Gestion_Stages.Repository;


import com.OCP.Gestion_Stages.domain.model.Stagiaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StagiaireRepository extends JpaRepository<Stagiaire, Long> {
    Optional<Stagiaire> findByEmail(String email);
    Optional<Stagiaire> findByCin(String cin);
    Optional<Stagiaire> findByUserId(Long userId);
    List<Stagiaire> findByEtablissementId(Long etablissementId);
    boolean existsByEmail(String email);
    List<Stagiaire> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(String nom, String prenom);
    @Query("SELECT s FROM Stagiaire s WHERE " +
            "LOWER(s.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.prenom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Stagiaire> searchByKeyword(String keyword);
    long countByDepartementId(Long departementId);

    @Query(value = """
        SELECT s.* FROM stagiaire s
        LEFT JOIN departement d ON d.id = s.departement_id
        LEFT JOIN etablissement e ON e.id = s.etablissement_id
        WHERE (CAST(:keyword AS VARCHAR) IS NULL
            OR LOWER(s.nom) LIKE LOWER(CONCAT('%', CAST(:keyword AS VARCHAR), '%'))
            OR LOWER(s.prenom) LIKE LOWER(CONCAT('%', CAST(:keyword AS VARCHAR), '%'))
            OR LOWER(s.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS VARCHAR), '%'))
            OR LOWER(s.cin) LIKE LOWER(CONCAT('%', CAST(:keyword AS VARCHAR), '%')))
        AND (CAST(:niveau AS VARCHAR) IS NULL OR s.niveau = CAST(:niveau AS VARCHAR))
        AND (CAST(:filiere AS VARCHAR) IS NULL OR s.filiere = CAST(:filiere AS VARCHAR))
        AND (CAST(:departementId AS BIGINT) IS NULL OR d.id = CAST(:departementId AS BIGINT))
        AND (CAST(:etatStage AS VARCHAR) IS NULL
            OR (CAST(:etatStage AS VARCHAR) = 'ACTIF' AND EXISTS (
                SELECT 1 FROM stage st WHERE st.stagiaire_id = s.id
                AND st.statut IN ('EN_COURS','CONVENTION_SIGNEE','EN_ATTENTE_EVALUATION')))
            OR (CAST(:etatStage AS VARCHAR) = 'TERMINE' AND EXISTS (
                SELECT 1 FROM stage st WHERE st.stagiaire_id = s.id
                AND st.statut = 'TERMINE')
                AND NOT EXISTS (
                SELECT 1 FROM stage st2 WHERE st2.stagiaire_id = s.id
                AND st2.statut IN ('EN_COURS','CONVENTION_SIGNEE','EN_ATTENTE_EVALUATION'))))
        """, nativeQuery = true)
    Page<Stagiaire> rechercher(
            @Param("keyword") String keyword,
            @Param("niveau") String niveau,
            @Param("filiere") String filiere,
            @Param("departementId") Long departementId,
            @Param("etatStage") String etatStage,
            Pageable pageable);
}