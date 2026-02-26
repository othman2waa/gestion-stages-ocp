package com.OCP.Gestion_Stages.domain.dto.evaluation;


import com.OCP.Gestion_Stages.domain.enums.TypeEvaluation;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class EvaluationRequest {

    @NotNull(message = "Le stage est obligatoire")
    private Long stageId;

    @NotNull(message = "L'encadrant est obligatoire")
    private Long encadrantId;

    @DecimalMin(value = "0.0", message = "La note minimum est 0")
    @DecimalMax(value = "20.0", message = "La note maximum est 20")
    private BigDecimal note;

    private String commentaire;

    @NotNull(message = "Le type d'évaluation est obligatoire")
    private TypeEvaluation typeEval;
}