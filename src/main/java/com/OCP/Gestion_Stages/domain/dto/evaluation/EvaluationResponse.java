package com.OCP.Gestion_Stages.domain.dto.evaluation;


import com.OCP.Gestion_Stages.domain.enums.TypeEvaluation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResponse {
    private Long id;
    private Long stageId;
    private String encadrantNom;
    private BigDecimal note;
    private String commentaire;
    private TypeEvaluation typeEval;
    private LocalDate dateEval;
}