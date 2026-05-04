package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Repository.ConventionRepository;
import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.Service.ConventionPdfService;
import com.OCP.Gestion_Stages.domain.enums.ConventionStatus;
import com.OCP.Gestion_Stages.domain.model.Convention;
import com.OCP.Gestion_Stages.domain.model.Stage;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.OCP.Gestion_Stages.domain.dto.convention.ConventionResponse;




@RestController
@RequestMapping("/api/conventions")
@RequiredArgsConstructor
@Transactional
public class ConventionController {

    private final ConventionRepository conventionRepository;
    private final StageRepository stageRepository;
    private final ConventionPdfService pdfService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<List<?>> getAll() {
        return ResponseEntity.ok(
                conventionRepository.findAllByOrderByCreatedAtDesc()
                        .stream().map(this::toResponse).collect(Collectors.toList())
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(conventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Convention introuvable"))));
    }

    @PostMapping("/generer/{stageId}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<?> generer(@PathVariable Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable"));
        if (conventionRepository.findByStageId(stageId).isPresent())
            return ResponseEntity.badRequest().body(Map.of("message","Convention déjà générée"));
        Convention c = Convention.builder()
                .stage(stage).statut(ConventionStatus.EN_VALIDATION)
                .dateEmission(LocalDate.now())
                .numero("CONV-" + stageId + "-" + LocalDate.now().getYear())
                .build();
        stage.setStatut(com.OCP.Gestion_Stages.domain.enums.StageStatus.CONVENTION_GENEREE);
        stageRepository.save(stage);
        return ResponseEntity.ok(toResponse(conventionRepository.save(c)));
    }

    // ── Signature électronique par rôle
    @PostMapping("/{id}/signer-electronique")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT','STAGIAIRE')")
    public ResponseEntity<?> signerElectronique(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        Convention c = conventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Convention introuvable"));

        String role = userDetails.getAuthorities().stream()
                .findFirst().map(a -> a.getAuthority().replace("ROLE_","")).orElse("");

        String base64 = body.get("signature").replace("data:image/png;base64,","");
        byte[] signatureBytes = java.util.Base64.getDecoder().decode(base64);

        switch (role) {
            case "STAGIAIRE" -> {
                c.setSignatureStagiaire(signatureBytes);
                c.setDateSignatureStagiaire(LocalDateTime.now());
            }
            case "ENCADRANT" -> {
                c.setSignatureEncadrant(signatureBytes);
                c.setDateSignatureEncadrant(LocalDateTime.now());
            }
            default -> {
                // Admin peut choisir quelle signature ajouter
                String cible = body.getOrDefault("cible", "stagiaire");
                if ("stagiaire".equals(cible)) {
                    c.setSignatureStagiaire(signatureBytes);
                    c.setDateSignatureStagiaire(LocalDateTime.now());
                } else {
                    c.setSignatureEncadrant(signatureBytes);
                    c.setDateSignatureEncadrant(LocalDateTime.now());
                }
            }
        }

        // Mise à jour statut signature
        boolean stagiaireSigne = c.getSignatureStagiaire() != null;
        boolean encadrantSigne = c.getSignatureEncadrant() != null;

        if (stagiaireSigne && encadrantSigne) {
            c.setStatutSignature("SIGNEE_COMPLET");
            c.setStatut(ConventionStatus.SIGNEE);
            Stage stage = c.getStage();
            stage.setStatut(com.OCP.Gestion_Stages.domain.enums.StageStatus.CONVENTION_SIGNEE);
            stageRepository.save(stage);
        } else if (stagiaireSigne || encadrantSigne) {
            c.setStatutSignature("PARTIELLEMENT_SIGNEE");
        }

        conventionRepository.save(c);

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("message", "Signature enregistrée avec succès");
        response.put("statutSignature", c.getStatutSignature());
        response.put("stagiaireSigne", stagiaireSigne);
        response.put("encadrantSigne", encadrantSigne);
        return ResponseEntity.ok(response);
    }

    // ── Statut des signatures
    @GetMapping("/{id}/signature-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSignatureStatus(@PathVariable Long id) {
        Convention c = conventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Convention introuvable"));
        Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("statutSignature", c.getStatutSignature());
        status.put("stagiaireSigne", c.getSignatureStagiaire() != null);
        status.put("encadrantSigne", c.getSignatureEncadrant() != null);
        status.put("dateSignatureStagiaire", c.getDateSignatureStagiaire() != null ?
                c.getDateSignatureStagiaire().toString() : null);
        status.put("dateSignatureEncadrant", c.getDateSignatureEncadrant() != null ?
                c.getDateSignatureEncadrant().toString() : null);
        return ResponseEntity.ok(status);
    }

    // ── Signer manuellement (admin simple)
    @PatchMapping("/{id}/signer")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<?> signer(@PathVariable Long id) {
        Convention c = conventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Convention introuvable"));
        c.setStatut(ConventionStatus.SIGNEE);
        c.setStatutSignature("SIGNEE_COMPLET");
        Stage stage = c.getStage();
        stage.setStatut(com.OCP.Gestion_Stages.domain.enums.StageStatus.CONVENTION_SIGNEE);
        stageRepository.save(stage);
        return ResponseEntity.ok(toResponse(conventionRepository.save(c)));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT','STAGIAIRE')")
    public ResponseEntity<byte[]> getPdf(@PathVariable Long id) {
        Convention c = conventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Convention introuvable"));

        // Construire ConventionResponse depuis l'entité
        ConventionResponse dto = new ConventionResponse();
        dto.setId(c.getId());
        dto.setNumero(c.getNumero());
        dto.setStatut(c.getStatut());
        dto.setDateEmission(c.getDateEmission());
        dto.setCreatedAt(c.getCreatedAt());

        Stage s = c.getStage();
        if (s != null) {
            dto.setStageId(s.getId());
            dto.setStageSujet(s.getSujet());
            dto.setTypeStage(s.getTypeStage() != null ? s.getTypeStage().name() : "");
            dto.setStageDebut(s.getDateDebut());
            dto.setStageFin(s.getDateFin());
            if (s.getStagiaire() != null) {
                dto.setStagiaireNom(s.getStagiaire().getPrenom() + " " + s.getStagiaire().getNom());
                dto.setStagiaireEmail(s.getStagiaire().getEmail());
                dto.setStagiaireCin(s.getStagiaire().getCin());
                dto.setStagiaireFiliere(s.getStagiaire().getFiliere());
                dto.setStagiaireNiveau(s.getStagiaire().getNiveau());
                if (s.getStagiaire().getEtablissement() != null)
                    dto.setStagiaireEtablissement(s.getStagiaire().getEtablissement().getNom());
            }
            if (s.getEncadrant() != null) {
                dto.setEncadrantNom(s.getEncadrant().getPrenom() + " " + s.getEncadrant().getNom());
                dto.setEncadrantEmail(s.getEncadrant().getEmail());
            }
            if (s.getDepartement() != null)
                dto.setDepartementNom(s.getDepartement().getNom());
        }

        byte[] pdf = pdfService.genererPdf(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"convention-" + c.getNumero() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<?> supprimer(@PathVariable Long id) {
        conventionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toResponse(Convention c) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("numero", c.getNumero());
        map.put("statut", c.getStatut());
        map.put("statutSignature", c.getStatutSignature() != null ? c.getStatutSignature() : "EN_ATTENTE");
        map.put("stagiaireSigne", c.getSignatureStagiaire() != null);
        map.put("encadrantSigne", c.getSignatureEncadrant() != null);
        map.put("dateEmission", c.getDateEmission() != null ? c.getDateEmission().toString() : "");
        map.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
        Stage s = c.getStage();
        map.put("stageId", s != null ? s.getId() : null);
        map.put("stagiaireNom", s != null && s.getStagiaire() != null ?
                s.getStagiaire().getPrenom() + " " + s.getStagiaire().getNom() : "");
        map.put("encadrantNom", s != null && s.getEncadrant() != null ?
                s.getEncadrant().getPrenom() + " " + s.getEncadrant().getNom() : "");
        map.put("sujet", s != null ? s.getSujet() : "");
        map.put("dateDebut", s != null && s.getDateDebut() != null ? s.getDateDebut().toString() : "");
        map.put("dateFin", s != null && s.getDateFin() != null ? s.getDateFin().toString() : "");
        return map;
    }
}