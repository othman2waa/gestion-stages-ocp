package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.pointage.PointageBulkRequest;
import com.OCP.Gestion_Stages.domain.dto.pointage.PointageResponse;

import java.util.List;

public interface PointageService {
    List<PointageResponse> saveAll(PointageBulkRequest request);
    List<PointageResponse> findByStage(Long stageId);
}
