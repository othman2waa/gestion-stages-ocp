package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.DocumentStagiaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DocumentStagiaireRepository extends JpaRepository<DocumentStagiaire, Long> {

    List<DocumentStagiaire> findByStagiaireId(Long stagiaireId);

    Optional<DocumentStagiaire> findByStagiaireIdAndTypeDocument(Long stagiaireId, String typeDocument);

    boolean existsByStagiaireIdAndTypeDocument(Long stagiaireId, String typeDocument);

    void deleteByStagiaireIdAndTypeDocument(Long stagiaireId, String typeDocument);

    @Query("SELECT d.id, d.typeDocument, d.nomFichier, d.contentType, d.taille, d.uploadedAt FROM DocumentStagiaire d WHERE d.stagiaire.id = :stagiaireId")
    List<Object[]> findMetaByStagiaireId(Long stagiaireId);

    // Migration bytea → disque : lignes encore stockées en base
    @Query("SELECT d.id FROM DocumentStagiaire d WHERE d.cheminFichier IS NULL AND d.contenu IS NOT NULL")
    List<Long> findIdsAMigrer();
}
