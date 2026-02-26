package com.OCP.Gestion_Stages.Service.interfaces;


import com.OCP.Gestion_Stages.domain.dto.evaluation.EvaluationRequest;
import com.OCP.Gestion_Stages.domain.dto.evaluation.EvaluationResponse;
import java.util.List;

public interface EvaluationService {
    EvaluationResponse createEvaluation(EvaluationRequest request);
    EvaluationResponse getEvaluationById(Long id);
    List<EvaluationResponse> getEvaluationsByStage(Long stageId);
    List<EvaluationResponse> getEvaluationsByEncadrant(Long encadrantId);
    EvaluationResponse updateEvaluation(Long id, EvaluationRequest request);
    void deleteEvaluation(Long id);
}