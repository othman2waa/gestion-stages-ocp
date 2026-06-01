package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.DocumentStagiaireRepository;
import com.OCP.Gestion_Stages.Repository.StagiaireRepository;
import com.OCP.Gestion_Stages.Repository.UserRepository;
import com.OCP.Gestion_Stages.Service.interfaces.DocumentStagiaireService;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.DocumentStagiaireResponse;
import com.OCP.Gestion_Stages.domain.model.DocumentStagiaire;
import com.OCP.Gestion_Stages.domain.model.Stagiaire;
import com.OCP.Gestion_Stages.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentStagiaireServiceImpl implements DocumentStagiaireService {

    private final DocumentStagiaireRepository documentRepository;
    private final StagiaireRepository stagiaireRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public DocumentStagiaireResponse upload(String username, String typeDocument, MultipartFile file) {
        Stagiaire stagiaire = resolveStagiaire(username);

        // Si un document du même type existe, on le remplace
        documentRepository.findByStagiaireIdAndTypeDocument(stagiaire.getId(), typeDocument)
                .ifPresent(existing -> documentRepository.delete(existing));

        try {
            DocumentStagiaire doc = DocumentStagiaire.builder()
                    .stagiaire(stagiaire)
                    .typeDocument(typeDocument)
                    .nomFichier(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .taille(file.getSize())
                    .contenu(file.getBytes())
                    .build();

            DocumentStagiaire saved = documentRepository.save(doc);
            return toResponse(saved);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier", e);
        }
    }

    @Override
    public List<DocumentStagiaireResponse> getMesDocuments(String username) {
        Stagiaire stagiaire = resolveStagiaire(username);
        return documentRepository.findByStagiaireId(stagiaire.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<DocumentStagiaireResponse> getDocumentsByStagiaireId(Long stagiaireId) {
        return documentRepository.findByStagiaireId(stagiaireId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public DocumentStagiaire download(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document introuvable"));
    }

    @Override
    @Transactional
    public void delete(String username, Long documentId) {
        Stagiaire stagiaire = resolveStagiaire(username);
        DocumentStagiaire doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document introuvable"));

        if (!doc.getStagiaire().getId().equals(stagiaire.getId())) {
            throw new RuntimeException("Vous ne pouvez supprimer que vos propres documents");
        }
        documentRepository.delete(doc);
    }

    private Stagiaire resolveStagiaire(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return stagiaireRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profil stagiaire introuvable"));
    }

    private DocumentStagiaireResponse toResponse(DocumentStagiaire doc) {
        return DocumentStagiaireResponse.builder()
                .id(doc.getId())
                .typeDocument(doc.getTypeDocument())
                .nomFichier(doc.getNomFichier())
                .contentType(doc.getContentType())
                .taille(doc.getTaille())
                .uploadedAt(doc.getUploadedAt())
                .build();
    }
}
