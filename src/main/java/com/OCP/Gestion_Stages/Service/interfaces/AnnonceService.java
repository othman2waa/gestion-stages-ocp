package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.annonce.AnnonceRequest;
import com.OCP.Gestion_Stages.domain.dto.annonce.AnnonceResponse;
import java.util.List;

public interface AnnonceService {
    AnnonceResponse create(AnnonceRequest request, String username);
    AnnonceResponse update(Long id, AnnonceRequest request);
    void delete(Long id);
    List<AnnonceResponse> findAll();
    List<AnnonceResponse> findActives();
    AnnonceResponse findById(Long id);
    void toggleActif(Long id);
}