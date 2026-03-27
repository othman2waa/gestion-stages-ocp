package com.OCP.Gestion_Stages.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidature")
@Data
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    private String email;

    private String telephone;
    private String filiere;
    private String niveau;
    private String etablissement;

    @Column(name = "sujet_souhaite", columnDefinition = "TEXT")
    private String sujetSouhaite;

    @Column(name = "departement_souhaite")
    private String departementSouhaite;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String statut = "EN_ATTENTE";

    @Column(name = "commentaire_rh", columnDefinition = "TEXT")
    private String commentaireRh;

    @Column(name = "cv_contenu", columnDefinition = "bytea")
    private byte[] cvContenu;

    @Column(name = "cv_nom_fichier")
    private String cvNomFichier;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "traite_at")
    private LocalDateTime traiteAt;

    @Column(name = "traite_par")
    private String traitePar;
    @Column(name = "annonce_id")
    private Long annonceId;

    @Column(name = "score_matching")
    private Integer scoreMatching;

    @Column(name = "competences_extraites", columnDefinition = "TEXT")
    private String competencesExtraites;
    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}