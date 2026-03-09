package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.stage.StageRequest;
import com.OCP.Gestion_Stages.domain.dto.stage.StageResponse;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface StageService {
    List<StageResponse> findAll();
    StageResponse findById(Long id);
    StageResponse create(StageRequest request);
    StageResponse update(Long id, StageRequest request);
    void delete(Long id);
    StageResponse updateStatut(Long id, StageStatus statut);
    List<StageResponse> findByStagiaire(Long stagiaireId);
    List<StageResponse> findByEncadrant(Long encadrantId);
    Page<StageResponse> rechercher(String keyword, StageStatus statut, String typeStage, Long departementId, Pageable pageable);

}