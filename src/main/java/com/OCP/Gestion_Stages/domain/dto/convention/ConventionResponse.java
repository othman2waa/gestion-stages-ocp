package com.OCP.Gestion_Stages.domain.dto.convention;

import com.OCP.Gestion_Stages.domain.enums.ConventionStatus;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ConventionResponse {
    private Long id;
    private Long stageId;
    private String stageSujet;
    private String numero;
    private ConventionStatus statut;
    private String cheminFichier;
    private LocalDate dateEmission;
    private LocalDateTime createdAt;
    // Infos stagiaire
    private String stagiaireNom;
    private String stagiaireEmail;
    private String stagiaireCin;
    private String stagiaireFiliere;
    private String stagiaireNiveau;
    private String stagiaireEtablissement;

    // Infos encadrant
    private String encadrantNom;
    private String encadrantEmail;

    // Infos stage
    private String departementNom;
    private java.time.LocalDate stageDebut;
    private java.time.LocalDate stageFin;
    private String typeStage;
}