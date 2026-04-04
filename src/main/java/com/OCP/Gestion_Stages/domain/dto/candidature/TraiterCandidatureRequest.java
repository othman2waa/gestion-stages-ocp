package com.OCP.Gestion_Stages.domain.dto.candidature;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TraiterCandidatureRequest {
    private String statut;
    private String commentaireRh;
    private Long encadrantId;
    private Long departementId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String typeStage;
    private String sujet;
}