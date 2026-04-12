package com.OCP.Gestion_Stages.Repository;


import com.OCP.Gestion_Stages.domain.model.Stagiaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}