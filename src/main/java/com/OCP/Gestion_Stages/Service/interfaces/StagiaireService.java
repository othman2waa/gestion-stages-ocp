package com.OCP.Gestion_Stages.Service.interfaces;


import com.OCP.Gestion_Stages.domain.dto.stagiaire.StagiaireRequest;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.StagiaireResponse;
import java.util.List;

public interface StagiaireService {
    StagiaireResponse createStagiaire(StagiaireRequest request);
    StagiaireResponse getStagiaireById(Long id);
    StagiaireResponse getStagiaireByEmail(String email);
    List<StagiaireResponse> getAllStagiaires();
    List<StagiaireResponse> searchStagiaires(String keyword);
    StagiaireResponse updateStagiaire(Long id, StagiaireRequest request);
    void deleteStagiaire(Long id);
}