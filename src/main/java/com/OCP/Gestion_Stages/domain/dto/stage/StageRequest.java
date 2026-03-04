package com.OCP.Gestion_Stages.domain.dto.stage;

import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.enums.TypeStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class StageRequest {
    @NotNull
    private Long stagiaireId;
    private Long encadrantId;
    private Long departementId;
    @NotBlank
    private String sujet;
    @NotNull
    private TypeStage typeStage;
    private StageStatus statut;
    private LocalDate dateDebut;
    private LocalDate dateFin;
}