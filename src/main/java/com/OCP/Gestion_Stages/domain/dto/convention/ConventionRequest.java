package com.OCP.Gestion_Stages.domain.dto.convention;

import com.OCP.Gestion_Stages.domain.enums.ConventionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ConventionRequest {
    @NotNull
    private Long stageId;
    private String numero;
    private ConventionStatus statut;
    private String cheminFichier;
    private LocalDate dateEmission;
}