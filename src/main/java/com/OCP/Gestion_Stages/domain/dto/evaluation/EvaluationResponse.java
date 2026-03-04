package com.OCP.Gestion_Stages.domain.dto.evaluation;

import com.OCP.Gestion_Stages.domain.enums.TypeEvaluation;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EvaluationResponse {
    private Long id;
    private Long stageId;
    private String stageSujet;
    private Long encadrantId;
    private String encadrantNom;
    private Double note;
    private String commentaire;
    private TypeEvaluation typeEvaluation;
    private LocalDate dateEval;
    private LocalDateTime createdAt;
}