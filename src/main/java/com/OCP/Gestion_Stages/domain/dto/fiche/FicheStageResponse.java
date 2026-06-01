package com.OCP.Gestion_Stages.domain.dto.fiche;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FicheStageResponse {
    private Long id;
    private Long stageId;
    private String stageSujet;
    private String stagiaireNom;
    private Long encadrantId;
    private String encadrantNom;
    private Integer noteGlobale;
    private Integer qualiteTravail;
    private Integer respectDelais;
    private Integer initiative;
    private Integer qualiteRapport;
    private Integer competencesTechniques;
    private String recommandation;
    private String commentaires;
    private Double moyenneGenerale;
    private LocalDateTime createdAt;
}
