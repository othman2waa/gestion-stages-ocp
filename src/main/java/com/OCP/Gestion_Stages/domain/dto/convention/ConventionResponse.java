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
}