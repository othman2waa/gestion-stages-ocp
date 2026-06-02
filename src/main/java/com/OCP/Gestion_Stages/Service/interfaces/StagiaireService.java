package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.stagiaire.MonDashboardResponse;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.StagiaireRequest;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.StagiaireResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface StagiaireService {
    List<StagiaireResponse> findAll();
    StagiaireResponse findById(Long id);
    StagiaireResponse create(StagiaireRequest request);
    StagiaireResponse update(Long id, StagiaireRequest request);
    MonDashboardResponse getMonDashboard(String username);
    void delete(Long id);
    List<StagiaireResponse> search(String keyword);
    Page<StagiaireResponse> rechercher(String keyword, String niveau, String filiere, Long departementId, Pageable pageable);

    void activerCompte(Long stagiaireId);
    void desactiverCompte(Long stagiaireId);
    String resetPassword(Long stagiaireId);
    List<StagiaireResponse> findAllAvecComptes();
    List<StagiaireResponse> getMesStagiaires(String username);
    List<StagiaireResponse> findByDepartement(Long departementId);
}