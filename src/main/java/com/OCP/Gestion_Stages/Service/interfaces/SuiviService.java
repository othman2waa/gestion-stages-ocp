package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.suivi.SuiviRequest;
import com.OCP.Gestion_Stages.domain.dto.suivi.SuiviResponse;
import java.util.List;

public interface SuiviService {
    SuiviResponse create(SuiviRequest request, String username);
    SuiviResponse update(Long id, SuiviRequest request);
    void delete(Long id);
    List<SuiviResponse> findByStage(Long stageId);
    List<SuiviResponse> findByEncadrant(String username);
}