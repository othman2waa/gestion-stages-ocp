package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.StageHistoriqueResponse;
import com.OCP.Gestion_Stages.domain.dto.WorkflowRequest;
import java.util.List;

public interface WorkflowService {
    void transitionner(Long stageId, WorkflowRequest request);
    List<StageHistoriqueResponse> getHistorique(Long stageId);
    List<String> getTransitionsPossibles(Long stageId);
}