package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.ConventionRepository;
import com.OCP.Gestion_Stages.Repository.DocumentStagiaireRepository;
import com.OCP.Gestion_Stages.Repository.StagiaireRepository;
import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.Repository.UserRepository;
import com.OCP.Gestion_Stages.Service.FileStorageService;
import com.OCP.Gestion_Stages.Service.interfaces.ConventionServiceExtended;
import com.OCP.Gestion_Stages.Service.interfaces.DocumentStagiaireService;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.DocumentStagiaireResponse;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.model.DocumentStagiaire;
import com.OCP.Gestion_Stages.domain.model.Stagiaire;
import com.OCP.Gestion_Stages.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentStagiaireServiceImpl implements DocumentStagiaireService {

    private static final List<String> REQUIRED_DOCS = List.of("CV", "CIN", "PHOTO", "DIPLOME");

    private final DocumentStagiaireRepository documentRepository;
    private final StagiaireRepository stagiaireRepository;
    private final UserRepository userRepository;
    private final StageRepository stageRepository;
    private final ConventionRepository conventionRepository;
    private final ConventionServiceExtended conventionServiceExtended;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public DocumentStagiaireResponse upload(String username, String typeDocument, MultipartFile file) {
        Stagiaire stagiaire = resolveStagiaire(username);

        // Si un document du même type existe, on le remplace (et on supprime son fichier disque)
        documentRepository.findByStagiaireIdAndTypeDocument(stagiaire.getId(), typeDocument)
                .ifPresent(existing -> {
                    fileStorageService.delete(existing.getCheminFichier());
                    documentRepository.delete(existing);
                });

        try {
            String chemin = fileStorageService.store(file.getBytes(), file.getOriginalFilename());
            DocumentStagiaire doc = DocumentStagiaire.builder()
                    .stagiaire(stagiaire)
                    .typeDocument(typeDocument)
                    .nomFichier(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .taille(file.getSize())
                    .cheminFichier(chemin)
                    .build();

            DocumentStagiaire saved = documentRepository.save(doc);

            // Auto-génération convocation si dossier complet + stage VALIDEE
            tryAutoGenererConvocation(stagiaire);

            return toResponse(saved);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier", e);
        }
    }

    @Override
    public List<DocumentStagiaireResponse> getMesDocuments(String username) {
        Stagiaire stagiaire = resolveStagiaire(username);
        // Projection métadonnées seulement — ne charge pas le byte[] contenu en RAM
        return documentRepository.findMetaByStagiaireId(stagiaire.getId())
                .stream().map(this::toResponseMeta).collect(Collectors.toList());
    }

    @Override
    public List<DocumentStagiaireResponse> getDocumentsByStagiaireId(Long stagiaireId) {
        // Projection métadonnées seulement — ne charge pas le byte[] contenu en RAM
        return documentRepository.findMetaByStagiaireId(stagiaireId)
                .stream().map(this::toResponseMeta).collect(Collectors.toList());
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
        fileStorageService.delete(doc.getCheminFichier());
        documentRepository.delete(doc);
    }

    @Override
    public boolean isDossierComplet(Long stagiaireId) {
        return REQUIRED_DOCS.stream()
                .allMatch(type -> documentRepository.existsByStagiaireIdAndTypeDocument(stagiaireId, type));
    }

    private void tryAutoGenererConvocation(Stagiaire stagiaire) {
        try {
            if (!isDossierComplet(stagiaire.getId())) return;

            stageRepository.findByStagiaireId(stagiaire.getId()).stream()
                    .filter(s -> s.getStatut() == StageStatus.VALIDEE)
                    .filter(s -> conventionRepository.findByStageId(s.getId()).isEmpty())
                    .forEach(s -> {
                        conventionServiceExtended.generer(s.getId());
                        log.info("Convocation auto-générée pour stage {} (stagiaire {})",
                                s.getId(), stagiaire.getPrenom() + " " + stagiaire.getNom());
                    });
        } catch (Exception e) {
            log.warn("Erreur auto-génération convocation: {}", e.getMessage());
        }
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

    // Mapping depuis la projection [id, typeDocument, nomFichier, contentType, taille, uploadedAt]
    private DocumentStagiaireResponse toResponseMeta(Object[] r) {
        return DocumentStagiaireResponse.builder()
                .id((Long) r[0])
                .typeDocument((String) r[1])
                .nomFichier((String) r[2])
                .contentType((String) r[3])
                .taille((Long) r[4])
                .uploadedAt((java.time.LocalDateTime) r[5])
                .build();
    }
}
