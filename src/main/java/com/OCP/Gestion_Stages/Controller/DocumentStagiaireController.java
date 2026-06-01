package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Service.interfaces.DocumentStagiaireService;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.DocumentStagiaireResponse;
import com.OCP.Gestion_Stages.domain.model.DocumentStagiaire;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents-stagiaire")
@RequiredArgsConstructor
public class DocumentStagiaireController {

    private final DocumentStagiaireService documentService;

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
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        doc.getContentType() != null ? doc.getContentType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getNomFichier() + "\"")
                .body(doc.getContenu());
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
