package com.OCP.Gestion_Stages.Service.interfaces;


import com.OCP.Gestion_Stages.domain.dto.stagiaire.StagiaireResponse;
import com.OCP.Gestion_Stages.domain.model.Encadrant;
import java.util.List;

public interface EncadrantService {
    Encadrant getEncadrantById(Long id);
    Encadrant getEncadrantByEmail(String email);
    List<Encadrant> getAllEncadrants();
    List<Encadrant> getEncadrantsByDepartement(Long departementId);
    List<StagiaireResponse> getStagiairesByEncadrant(Long encadrantId);
}