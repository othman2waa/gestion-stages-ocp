package com.OCP.Gestion_Stages.domain.dto.stagiaire;

import com.OCP.Gestion_Stages.domain.enums.ConventionStatus;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import lombok.Data;
import java.time.LocalDate;

@Data
public class MonDashboardResponse {
    // Infos stagiaire
    private String stagiaireNom;
    private String stagiairePrenom;
    private String stagiaireEmail;
    private String stagiaireFiliere;
    private String stagiaireNiveau;

    // Infos stage
    private Long stageId;
    private String stageSujet;
    private String typeStage;
    private StageStatus stageStatut;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String departementNom;

    // Encadrant OCP
    private String encadrantNom;
    private String encadrantEmail;

    // Convention
    private Long conventionId;
    private String conventionNumero;
    private ConventionStatus conventionStatut;
    private LocalDate conventionDateEmission;

    // Progression (0-100)
    private int progression;
}