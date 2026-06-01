package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.fiche.*;

import java.util.List;

public interface FicheAppreciationService {

    // Fiche stage
    FicheStageResponse createFicheStage(FicheStageRequest request, String username);
    FicheStageResponse updateFicheStage(Long id, FicheStageRequest request);
    FicheStageResponse getFicheStageByStageId(Long stageId);
    List<FicheStageResponse> getAllFichesStage();
    List<FicheStageResponse> getMesFichesStage(String username);

    // Fiche stagiaire
    FicheStagiaireResponse createFicheStagiaire(FicheStagiaireRequest request, String username);
    FicheStagiaireResponse updateFicheStagiaire(Long id, FicheStagiaireRequest request);
    FicheStagiaireResponse getFicheStagiaireByStageId(Long stageId);
    List<FicheStagiaireResponse> getAllFichesStagiaire();
    List<FicheStagiaireResponse> getMesFichesStagiaire(String username);
}
