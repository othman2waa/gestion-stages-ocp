package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.attestation.AttestationDTO;
import java.util.List;
import java.util.Map;

public interface AttestationServiceExtended {
    AttestationDTO demander(Long stageId);
    AttestationDTO getMaDemande(Long stageId);
    List<AttestationDTO> getAll();
    List<AttestationDTO> getEnAttente();
    AttestationDTO approuver(Long id, String username);
    AttestationDTO refuser(Long id, String commentaire, String username);
    AttestationDTO toDTO(com.OCP.Gestion_Stages.domain.model.AttestationStage att);
}