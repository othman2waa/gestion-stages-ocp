package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Repository.DepartementRepository;
import com.OCP.Gestion_Stages.Service.interfaces.CandidatureService;
import com.OCP.Gestion_Stages.Service.interfaces.CandidatureServiceExtended;
import com.OCP.Gestion_Stages.domain.dto.candidature.CandidatureDTO;
import com.OCP.Gestion_Stages.domain.dto.candidature.CandidatureRequest;
import com.OCP.Gestion_Stages.domain.dto.candidature.CandidatureResponse;
import com.OCP.Gestion_Stages.domain.dto.candidature.TraiterCandidatureRequest;
import com.OCP.Gestion_Stages.Repository.CandidatureRepository;
import com.OCP.Gestion_Stages.Repository.DocumentCandidatureRepository;
import com.OCP.Gestion_Stages.domain.model.DocumentCandidature;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/candidatures")
@RequiredArgsConstructor
@Transactional
public class CandidatureController {

    // ── Services (logique métier déléguée)
    private final CandidatureService candidatureService;
    private final CandidatureServiceExtended candidatureServiceExtended;

    // ── Repositories (uniquement pour les opérations simples restantes)
    private final CandidatureRepository candidatureRepository;
    private final DepartementRepository departementRepository;
    private final DocumentCandidatureRepository documentRepository;

    // ════════════════════════════════════════
    // PUBLIC — Soumettre candidature
    // ════════════════════════════════════════
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> soumettre(
            @RequestPart("data") CandidatureRequest request,
            @RequestPart(value = "cv", required = false) MultipartFile cv) throws Exception {

        // Vérification email unique
        if (candidatureRepository.findAll().stream()
                .anyMatch(c -> c.getEmail().equals(request.getEmail()))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Une candidature avec cet email existe déjà"));
        }

        // Soumission via service existant
        CandidatureResponse response = candidatureService.soumettre(request, cv);

        // Liaison département
        if (request.getDepartementId() != null) {
            candidatureRepository.findById(response.getId()).ifPresent(c -> {
                departementRepository.findById(request.getDepartementId())
                        .ifPresent(c::setDepartement);
                candidatureRepository.save(c);
            });
        }
        return ResponseEntity.ok(response);
    }

    // ════════════════════════════════════════
    // ADMIN RH — Lecture
    // ════════════════════════════════════════
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<CandidatureResponse>> getAll() {
        return ResponseEntity.ok(candidatureService.findAll());
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<CandidatureResponse>> getByStatut(@PathVariable String statut) {
        return ResponseEntity.ok(candidatureService.findByStatut(statut));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<CandidatureResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(candidatureService.findById(id));
    }

    @GetMapping("/{id}/cv")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<byte[]> getCv(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=cv.pdf")
                .body(candidatureService.getCv(id));
    }

    // ════════════════════════════════════════
    // ENCADRANT — Délégué au service
    // ════════════════════════════════════════
    @GetMapping("/mon-departement")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<List<CandidatureDTO>> getCandidaturesDepartement(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                candidatureServiceExtended.getCandidaturesDepartement(userDetails.getUsername())
        );
    }

    @PatchMapping("/{id}/planifier-meeting")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<CandidatureDTO> planifierMeeting(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                candidatureServiceExtended.planifierMeeting(id, body.get("dateMeeting"))
        );
    }

    @PatchMapping("/{id}/decision-encadrant")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<CandidatureDTO> decisionEncadrant(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        return ResponseEntity.ok(
                candidatureServiceExtended.decisionEncadrant(
                        id, body.get("decision"), body.get("note"), userDetails.getUsername()
                )
        );
    }

    // ════════════════════════════════════════
    // STAGIAIRE — Upload documents
    // ════════════════════════════════════════
    @PostMapping("/{id}/upload-document")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<?> uploadDocument(
            @PathVariable Long id,
            @RequestParam("type") String typeDocument,
            @RequestParam("fichier") MultipartFile fichier) throws Exception {
        var c = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));

        // Supprimer ancien document même type
        documentRepository.findByCandidatureId(id).stream()
                .filter(d -> d.getTypeDocument().equals(typeDocument))
                .forEach(documentRepository::delete);

        // Sauvegarder nouveau document
        documentRepository.save(DocumentCandidature.builder()
                .candidature(c).typeDocument(typeDocument)
                .nomFichier(fichier.getOriginalFilename())
                .contenu(fichier.getBytes()).statutIa("NON_VERIFIE").build());

        long nbDocs = documentRepository.findByCandidatureId(id).size();
        if (nbDocs >= 4) { c.setStatut("DOCUMENTS_SOUMIS"); candidatureRepository.save(c); }

        return ResponseEntity.ok(Map.of(
                "message", "Document uploadé",
                "type", typeDocument,
                "nbDocuments", nbDocs
        ));
    }

    // ════════════════════════════════════════
    // RH ADMIN — Documents + IA + Validation
    // ════════════════════════════════════════
    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<?>> getDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(
                documentRepository.findByCandidatureId(id).stream().map(d -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", d.getId());
                    m.put("typeDocument", d.getTypeDocument());
                    m.put("nomFichier", d.getNomFichier());
                    m.put("uploadedAt", d.getUploadedAt() != null ? d.getUploadedAt().toString() : "");
                    m.put("statutIa", d.getStatutIa());
                    m.put("scoreIa", d.getScoreIa());
                    m.put("commentaireIa", d.getCommentaireIa());
                    return m;
                }).collect(Collectors.toList())
        );
    }

    @PostMapping("/{id}/verifier-ia")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<Map<String, Object>> verifierIa(@PathVariable Long id) {
        return ResponseEntity.ok(candidatureServiceExtended.verifierIa(id));
    }

    @PatchMapping("/{id}/valider-final")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<CandidatureDTO> validerFinal(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                candidatureServiceExtended.validerFinal(
                        id, body.get("decision"), body.get("commentaire"), userDetails.getUsername()
                )
        );
    }

    // ════════════════════════════════════════
    // Ancien endpoint — compatibilité
    // ════════════════════════════════════════
    @PatchMapping("/{id}/traiter")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<CandidatureResponse> traiter(
            @PathVariable Long id,
            @RequestBody TraiterCandidatureRequest request,
            @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        return ResponseEntity.ok(
                candidatureService.traiter(id, request, userDetails.getUsername())
        );
    }
}