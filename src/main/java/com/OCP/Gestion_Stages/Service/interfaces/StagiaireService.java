package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.stagiaire.StagiaireRequest;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.StagiaireResponse;
import java.util.List;

public interface StagiaireService {
    List<StagiaireResponse> findAll();
    StagiaireResponse findById(Long id);
    StagiaireResponse create(StagiaireRequest request);
    StagiaireResponse update(Long id, StagiaireRequest request);
    void delete(Long id);
    List<StagiaireResponse> search(String keyword);
}