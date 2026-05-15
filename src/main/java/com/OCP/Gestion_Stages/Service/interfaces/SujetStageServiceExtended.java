package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.sujet.SujetStageDTO;
import java.util.List;
import java.util.Map;

public interface SujetStageServiceExtended {
    SujetStageDTO proposer(Map<String, Object> body, String username);
    List<SujetStageDTO> getAll();
    List<SujetStageDTO> getByStatut(String statut);
    List<SujetStageDTO> getMesSujets(String username);
    SujetStageDTO valider(Long id, String username);
    SujetStageDTO refuser(Long id);
    SujetStageDTO affecter(Long id, Map<String, Object> body, String username);
    SujetStageDTO toDTO(com.OCP.Gestion_Stages.domain.model.SujetStage s);
}