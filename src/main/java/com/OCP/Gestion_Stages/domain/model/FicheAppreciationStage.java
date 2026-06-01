package com.OCP.Gestion_Stages.domain.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "fiche_appreciation_stage", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"stage_id"})
})
@Data
public class FicheAppreciationStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encadrant_id", nullable = false)
    private Encadrant encadrant;

    @Column(name = "note_globale")
    private Integer noteGlobale;

    @Column(name = "qualite_travail")
    private Integer qualiteTravail;

    @Column(name = "respect_delais")
    private Integer respectDelais;

    private Integer initiative;

    @Column(name = "qualite_rapport")
    private Integer qualiteRapport;

    @Column(name = "competences_techniques")
    private Integer competencesTechniques;

    @Column(length = 50)
    private String recommandation;

    @Column(columnDefinition = "TEXT")
    private String commentaires;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
