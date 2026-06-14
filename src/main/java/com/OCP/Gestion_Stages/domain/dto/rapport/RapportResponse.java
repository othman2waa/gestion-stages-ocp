package com.OCP.Gestion_Stages.domain.dto.rapport;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RapportResponse {
    private Long id;
    private Long stageId;
    private String stageSujet;
    private String stagiaireNom;
    private String nomFichier;
    private String typeContenu;
    private Long taille;
    private LocalDateTime uploadedAt;
    // Validation encadrant
    private String statut;                 // EN_ATTENTE, VALIDE, REFUSE
    private String commentaireValidation;
    private LocalDateTime valideAt;
}