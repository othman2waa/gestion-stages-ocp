package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.encadrant.EncadrantRequest;
import com.OCP.Gestion_Stages.domain.dto.encadrant.EncadrantResponse;
import com.OCP.Gestion_Stages.domain.dto.encadrant.MonProfilEncadrantResponse;

import java.util.List;

public interface EncadrantService {
    List<EncadrantResponse> findAll();
    EncadrantResponse findById(Long id);
    EncadrantResponse create(EncadrantRequest request);
    EncadrantResponse update(Long id, EncadrantRequest request);
    void delete(Long id);
    List<EncadrantResponse> findByDepartement(Long departementId);
    MonProfilEncadrantResponse getMonProfil(String username);
}