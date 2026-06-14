package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.departement.DepartementRequest;
import com.OCP.Gestion_Stages.domain.dto.departement.DepartementResponse;
import java.util.List;

public interface DepartementService {
    List<DepartementResponse> findAll();
    List<DepartementResponse> findActifs();
    DepartementResponse findById(Long id);
    DepartementResponse create(DepartementRequest request);
    DepartementResponse update(Long id, DepartementRequest request);
    void delete(Long id);
    void toggleActif(Long id);
}