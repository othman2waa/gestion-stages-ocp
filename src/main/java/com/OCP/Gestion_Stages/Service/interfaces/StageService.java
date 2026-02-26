package com.OCP.Gestion_Stages.Service.interfaces;


import com.OCP.Gestion_Stages.domain.dto.stage.StageRequest;
import com.OCP.Gestion_Stages.domain.dto.stage.StageResponse;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import java.util.List;

public interface StageService {
    StageResponse createStage(StageRequest request);
    StageResponse getStageById(Long id);
    List<StageResponse> getAllStages();
    List<StageResponse> getStagesByStagiaire(Long stagiaireId);
    List<StageResponse> getStagesByEncadrant(Long encadrantId);
    List<StageResponse> getStagesByStatut(StageStatus statut);
    StageResponse updateStage(Long id, StageRequest request);
    StageResponse updateStatut(Long id, StageStatus statut);
    void deleteStage(Long id);
}