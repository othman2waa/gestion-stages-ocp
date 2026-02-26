package com.OCP.Gestion_Stages.Service.interfaces;


import com.OCP.Gestion_Stages.domain.dto.convention.ConventionRequest;
import com.OCP.Gestion_Stages.domain.dto.convention.ConventionResponse;
import com.OCP.Gestion_Stages.domain.enums.ConventionStatus;
import java.util.List;

public interface ConventionService {
    ConventionResponse createConvention(ConventionRequest request);
    ConventionResponse getConventionById(Long id);
    ConventionResponse getConventionByStageId(Long stageId);
    List<ConventionResponse> getAllConventions();
    ConventionResponse updateStatut(Long id, ConventionStatus statut);
    void deleteConvention(Long id);
}