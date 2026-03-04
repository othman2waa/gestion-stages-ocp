package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.convention.ConventionRequest;
import com.OCP.Gestion_Stages.domain.dto.convention.ConventionResponse;
import java.util.List;

public interface ConventionService {
    List<ConventionResponse> findAll();
    ConventionResponse findById(Long id);
    ConventionResponse create(ConventionRequest request);
    ConventionResponse update(Long id, ConventionRequest request);
    void delete(Long id);
    ConventionResponse findByStage(Long stageId);
}