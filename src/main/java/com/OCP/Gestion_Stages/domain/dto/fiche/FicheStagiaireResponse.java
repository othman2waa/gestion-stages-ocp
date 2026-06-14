package com.OCP.Gestion_Stages.domain.dto.fiche;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FicheStagiaireResponse {
    private Long id;
    private Long stageId;
    private String stageSujet;
    private String stagiaireNom;
    private Long encadrantId;
    private String encadrantNom;
    private Integer assiduite;
    private Integer ponctualite;
    private Integer comportementProfessionnel;
    private Integer espritEquipe;
    private Integer communication;
    private Integer adaptation;
    private String recommandationEmbauche;
    private String commentaires;
    private Double moyenneGenerale;
    private LocalDateTime createdAt;
}
