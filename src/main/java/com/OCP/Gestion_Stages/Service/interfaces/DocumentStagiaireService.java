package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.stagiaire.DocumentStagiaireResponse;
import com.OCP.Gestion_Stages.domain.model.DocumentStagiaire;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentStagiaireService {

    DocumentStagiaireResponse upload(String username, String typeDocument, MultipartFile file);

    List<DocumentStagiaireResponse> getMesDocuments(String username);

    List<DocumentStagiaireResponse> getDocumentsByStagiaireId(Long stagiaireId);

    DocumentStagiaire download(Long documentId);

    void delete(String username, Long documentId);
}
