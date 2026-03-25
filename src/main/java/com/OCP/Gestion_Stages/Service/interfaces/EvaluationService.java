package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.evaluation.EvaluationRequest;
import com.OCP.Gestion_Stages.domain.dto.evaluation.EvaluationResponse;
import java.util.List;

public interface EvaluationService {
    List<EvaluationResponse> findAll();
    EvaluationResponse findById(Long id);
    EvaluationResponse create(EvaluationRequest request);
    EvaluationResponse update(Long id, EvaluationRequest request);
    void delete(Long id);
    List<EvaluationResponse> findByStage(Long stageId);
    List<EvaluationResponse> getMesEvaluationsStagiaire(String username);
    List<EvaluationResponse> getMesEvaluationsEncadrant(String username);
}