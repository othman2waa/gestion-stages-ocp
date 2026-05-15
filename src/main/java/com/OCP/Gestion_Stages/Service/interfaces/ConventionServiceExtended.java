package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.convention.ConventionDTO;
import com.OCP.Gestion_Stages.domain.model.Convention;
import java.util.List;
import java.util.Map;

public interface ConventionServiceExtended {
    List<ConventionDTO> getAllDTO();
    ConventionDTO getByIdDTO(Long id);
    ConventionDTO generer(Long stageId);
    ConventionDTO signerManuel(Long id);
    Map<String, Object> signerElectronique(Long id, String signature, String cible, String username);
    Map<String, Object> getSignatureStatus(Long id);
    ConventionDTO toDTO(Convention c);
}