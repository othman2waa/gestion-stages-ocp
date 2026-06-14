package com.OCP.Gestion_Stages.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "annonce_stage")
@Data
public class AnnonceStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "competences_requises", columnDefinition = "TEXT")
    private String competencesRequises;

    private String departement;

    @Column(name = "type_stage")
    private String typeStage;

    @Column(name = "niveau_requis")
    private String niveauRequis;

    @Column(name = "filiere_requise")
    private String filiereRequise;

    @Column(name = "nombre_postes")
    private Integer nombrePostes = 1;

    @Column(name = "date_limite")
    private LocalDate dateLimite;

    private Boolean actif = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_par")
    private String createdPar;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}