package com.OCP.Gestion_Stages.domain.dto.convention;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class ConventionDTO {
    private Long id;
    private String numero;
    private String statut;
    private String statutSignature;
    private Boolean stagiaireSigne;
    private Boolean encadrantSigne;
    private LocalDateTime dateSignatureStagiaire;
    private LocalDateTime dateSignatureEncadrant;
    private LocalDate dateEmission;
    private LocalDateTime createdAt;
    private Long stageId;
    private String stagiaireNom;
    private String encadrantNom;
    private String sujet;
    private String dateDebut;
    private String dateFin;
    private String entiteAccueil;
}