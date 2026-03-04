package com.OCP.Gestion_Stages.domain.dto.evaluation;

import com.OCP.Gestion_Stages.domain.enums.TypeEvaluation;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class EvaluationRequest {
    @NotNull
    private Long stageId;
    @NotNull
    private Long encadrantId;
    @DecimalMin("0.0") @DecimalMax("20.0")
    private Double note;
    private String commentaire;
    @NotNull
    private TypeEvaluation typeEvaluation;
    private LocalDate dateEval;
}