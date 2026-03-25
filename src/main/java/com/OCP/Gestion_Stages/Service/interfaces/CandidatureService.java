package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.candidature.CandidatureRequest;
import com.OCP.Gestion_Stages.domain.dto.candidature.CandidatureResponse;
import com.OCP.Gestion_Stages.domain.dto.candidature.TraiterCandidatureRequest;
import com.OCP.Gestion_Stages.domain.model.Candidature;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

public interface CandidatureService {
    CandidatureResponse soumettre(CandidatureRequest request, MultipartFile cv) throws IOException;
    CandidatureResponse traiter(Long id, TraiterCandidatureRequest request, String username) throws Exception;
    List<CandidatureResponse> findAll();
    List<CandidatureResponse> findByStatut(String statut);
    CandidatureResponse findById(Long id);
    byte[] getCv(Long id);
}