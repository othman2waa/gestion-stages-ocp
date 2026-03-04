package com.OCP.Gestion_Stages.domain.dto.stage;

import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.enums.TypeStage;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StageResponse {
    private Long id;
    private String sujet;
    private TypeStage typeStage;
    private StageStatus statut;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Long stagiaireId;
    private String stagiaireNom;
    private Long encadrantId;
    private String encadrantNom;
    private Long departementId;
    private String departementNom;
    private LocalDateTime createdAt;
}