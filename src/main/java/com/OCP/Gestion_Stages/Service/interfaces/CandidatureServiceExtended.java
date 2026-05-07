package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.candidature.CandidatureDTO;
import java.util.List;
import java.util.Map;

public interface CandidatureServiceExtended {
    CandidatureDTO planifierMeeting(Long id, String dateMeeting);
    CandidatureDTO decisionEncadrant(Long id, String decision, String note, String username) throws Exception;
    Map<String, Object> verifierIa(Long id);
    CandidatureDTO validerFinal(Long id, String decision, String commentaire, String username);
    List<CandidatureDTO> getCandidaturesDepartement(String username);
    CandidatureDTO toDTO(com.OCP.Gestion_Stages.domain.model.Candidature c);
}