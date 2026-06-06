package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Service.FileStorageService;
import com.OCP.Gestion_Stages.Service.interfaces.DocumentStagiaireService;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.DocumentStagiaireResponse;
import com.OCP.Gestion_Stages.domain.model.DocumentStagiaire;
import com.OCP.Gestion_Stages.Repository.DocumentStagiaireRepository;
import com.OCP.Gestion_Stages.Repository.StagiaireRepository;
import com.OCP.Gestion_Stages.domain.model.Stagiaire;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/documents-stagiaire")
@RequiredArgsConstructor
public class DocumentStagiaireController {

    private final DocumentStagiaireService documentService;
    private final StagiaireRepository stagiaireRepository;
    private final DocumentStagiaireRepository documentStagiaireRepository;
    private final FileStorageService fileStorageService;

    /**
     * Upload ou remplacer un document (CV, CIN, PHOTO, DIPLOME, ASSURANCE, AUTRE)
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<DocumentStagiaireResponse> upload(
            @RequestParam("type") String typeDocument,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(documentService.upload(userDetails.getUsername(), typeDocument, file));
    }

    /**
     * Lister mes documents (stagiaire connecté)
     */
    @GetMapping("/mes-documents")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<List<DocumentStagiaireResponse>> getMesDocuments(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(documentService.getMesDocuments(userDetails.getUsername()));
    }

    /**
     * Lister les documents d'un stagiaire (RH / encadrant)
     */
    @GetMapping("/stagiaire/{stagiaireId}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<List<DocumentStagiaireResponse>> getDocumentsByStagiaire(
            @PathVariable Long stagiaireId) {
        return ResponseEntity.ok(documentService.getDocumentsByStagiaireId(stagiaireId));
    }

    /**
     * Télécharger un document par son ID
     */
    @GetMapping("/{documentId}/download")
    @PreAuthorize("hasAnyRole('STAGIAIRE','ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<byte[]> download(@PathVariable Long documentId) {
        DocumentStagiaire doc = documentService.download(documentId);
        byte[] data = doc.getCheminFichier() != null
                ? fileStorageService.read(doc.getCheminFichier())
                : doc.getContenu();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        doc.getContentType() != null ? doc.getContentType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getNomFichier() + "\"")
                .body(data);
    }

    /**
     * Compliance : stats documents pour tous les stagiaires (RH)
     */
    @GetMapping("/compliance")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<Map<String, Object>> getComplianceStats() {
        List<Stagiaire> allStagiaires = stagiaireRepository.findAll();
        List<String> requiredTypes = List.of("CV", "CIN", "PHOTO", "DIPLOME");

        int totalStagiaires = allStagiaires.size();
        int dossierComplet = 0;
        List<Map<String, Object>> incomplete = new ArrayList<>();

        for (Stagiaire s : allStagiaires) {
            List<DocumentStagiaireResponse> docs = documentService.getDocumentsByStagiaireId(s.getId());
            Set<String> uploaded = new HashSet<>();
            for (DocumentStagiaireResponse d : docs) uploaded.add(d.getTypeDocument());

            List<String> missing = new ArrayList<>();
            for (String t : requiredTypes) { if (!uploaded.contains(t)) missing.add(t); }

            if (missing.isEmpty()) {
                dossierComplet++;
            } else {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("stagiaireId", s.getId());
                item.put("nom", s.getPrenom() + " " + s.getNom());
                item.put("email", s.getEmail());
                item.put("documentsManquants", missing);
                item.put("progression", Math.round((requiredTypes.size() - missing.size()) * 100.0 / requiredTypes.size()));
                incomplete.add(item);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalStagiaires", totalStagiaires);
        result.put("dossierComplet", dossierComplet);
        result.put("dossierIncomplet", totalStagiaires - dossierComplet);
        result.put("tauxConformite", totalStagiaires > 0 ? Math.round(dossierComplet * 100.0 / totalStagiaires) : 0);
        result.put("stagiairesIncomplets", incomplete);
        return ResponseEntity.ok(result);
    }

    /**
     * Supprimer un de mes documents
     */
    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<Void> delete(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        documentService.delete(userDetails.getUsername(), documentId);
        return ResponseEntity.noContent().build();
    }
}
