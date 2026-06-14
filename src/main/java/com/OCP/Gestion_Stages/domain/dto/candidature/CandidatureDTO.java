package com.OCP.Gestion_Stages.domain.dto.candidature;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CandidatureDTO {
    // Identité
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;

    // Formation
    private String filiere;
    private String niveau;
    private String etablissement;
    private String specialite;

    // Candidature
    private String sujetSouhaite;
    private String message;
    private String statut;
    private String statutMeeting;
    private LocalDateTime dateMeeting;
    private String noteEncadrant;
    private Integer scoreMatching;

    // Département
    private Long departementId;
    private String departementNom;

    // Compte créé
    private String username;
    private Boolean convocationEnvoyee;
    private LocalDateTime dateConvocation;

    // Documents
    private Long nbDocuments;

    // Dates
    private LocalDateTime createdAt;
    private LocalDateTime traiteAt;
    private String traitePar;
}
