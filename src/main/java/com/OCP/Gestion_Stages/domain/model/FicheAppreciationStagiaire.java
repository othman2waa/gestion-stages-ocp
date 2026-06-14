package com.OCP.Gestion_Stages.domain.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "fiche_appreciation_stagiaire", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"stage_id"})
})
@Data
public class FicheAppreciationStagiaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encadrant_id", nullable = false)
    private Encadrant encadrant;

    private Integer assiduite;

    private Integer ponctualite;

    @Column(name = "comportement_professionnel")
    private Integer comportementProfessionnel;

    @Column(name = "esprit_equipe")
    private Integer espritEquipe;

    private Integer communication;

    private Integer adaptation;

    @Column(name = "recommandation_embauche", length = 50)
    private String recommandationEmbauche;

    @Column(columnDefinition = "TEXT")
    private String commentaires;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
