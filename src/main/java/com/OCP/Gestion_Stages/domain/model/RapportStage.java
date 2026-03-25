package com.OCP.Gestion_Stages.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "rapport_stage")
@Data
public class RapportStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;

    @Column(name = "type_contenu", nullable = false)
    private String typeContenu;

    private Long taille;

    @Column(name = "contenu", nullable = false, columnDefinition = "bytea")
    private byte[] contenu;

    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() { this.uploadedAt = LocalDateTime.now(); }
}