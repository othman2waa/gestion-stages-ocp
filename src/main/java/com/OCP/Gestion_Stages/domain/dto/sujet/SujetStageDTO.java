package com.OCP.Gestion_Stages.domain.dto.sujet;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class SujetStageDTO {
    private Long id;
    private String titre;
    private String description;
    private String technologies;
    private String niveauRequis;
    private String typeStage;
    private String statut;
    private Long encadrantId;
    private String encadrantNom;
    private String departementNom;
    private Long stageId;
    private String validatedBy;
    private LocalDateTime createdAt;
}