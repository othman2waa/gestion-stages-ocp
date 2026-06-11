package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Repository.ConventionRepository;
import com.OCP.Gestion_Stages.Service.ConventionPdfService;
import com.OCP.Gestion_Stages.Service.interfaces.ConventionService;
import com.OCP.Gestion_Stages.Service.interfaces.ConventionServiceExtended;
import com.OCP.Gestion_Stages.domain.dto.convention.ConventionDTO;
import com.OCP.Gestion_Stages.domain.dto.convention.ConventionResponse;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conventions")
@RequiredArgsConstructor
@Transactional
public class ConventionController {

    // ── Services (logique déléguée)
    private final ConventionServiceExtended conventionServiceExtended;
    private final ConventionPdfService pdfService;

    // ── Repository (uniquement pour getPdf qui utilise ConventionResponse)
    private final ConventionRepository conventionRepository;

    // ════════════════════════════════════════
    // LECTURE
    // ════════════════════════════════════════
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<List<ConventionDTO>> getAll() {
        return ResponseEntity.ok(conventionServiceExtended.getAllDTO());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<ConventionDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(conventionServiceExtended.getByIdDTO(id));
    }

    // ── Service CRUD basique
    private final ConventionService conventionService;

    // ════════════════════════════════════════
    // CRÉATION / MISE À JOUR
    // ════════════════════════════════════════
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<ConventionDTO> create(@RequestBody Map<String, Object> body) {
        com.OCP.Gestion_Stages.domain.dto.convention.ConventionRequest req =
                new com.OCP.Gestion_Stages.domain.dto.convention.ConventionRequest();
        req.setStageId(Long.valueOf(body.get("stageId").toString()));
        req.setNumero(body.getOrDefault("numero", "").toString());
        if (body.containsKey("statut") && body.get("statut") != null) {
            req.setStatut(com.OCP.Gestion_Stages.domain.enums.ConventionStatus.valueOf(body.get("statut").toString()));
        }
        if (body.containsKey("dateEmission") && body.get("dateEmission") != null) {
            req.setDateEmission(java.time.LocalDate.parse(body.get("dateEmission").toString().substring(0, 10)));
        }
        var resp = conventionService.create(req);
        Convention c = conventionRepository.findById(resp.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Convention introuvable"));
        return ResponseEntity.ok(conventionServiceExtended.toDTO(c));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<ConventionDTO> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        com.OCP.Gestion_Stages.domain.dto.convention.ConventionRequest req =
                new com.OCP.Gestion_Stages.domain.dto.convention.ConventionRequest();
        if (body.containsKey("stageId")) req.setStageId(Long.valueOf(body.get("stageId").toString()));
        if (body.containsKey("numero")) req.setNumero(body.get("numero").toString());
        if (body.containsKey("statut") && body.get("statut") != null) {
            req.setStatut(com.OCP.Gestion_Stages.domain.enums.ConventionStatus.valueOf(body.get("statut").toString()));
        }
        if (body.containsKey("dateEmission") && body.get("dateEmission") != null) {
            req.setDateEmission(java.time.LocalDate.parse(body.get("dateEmission").toString().substring(0, 10)));
        }
        conventionService.update(id, req);
        Convention c = conventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Convention introuvable"));
        return ResponseEntity.ok(conventionServiceExtended.toDTO(c));
    }

    // ════════════════════════════════════════
    // GÉNÉRATION + SIGNATURE
    // ════════════════════════════════════════
    @PostMapping("/generer/{stageId}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<?> generer(@PathVariable Long stageId) {
        if (conventionRepository.findByStageId(stageId).isPresent())
            return ResponseEntity.badRequest().body(Map.of("message", "Convention déjà générée"));
        return ResponseEntity.ok(conventionServiceExtended.generer(stageId));
    }

    @PatchMapping("/{id}/signer")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<ConventionDTO> signer(@PathVariable Long id) {
        return ResponseEntity.ok(conventionServiceExtended.signerManuel(id));
    }

    @PostMapping("/{id}/signer-electronique")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT','STAGIAIRE')")
    public ResponseEntity<Map<String, Object>> signerElectronique(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        String role = userDetails.getAuthorities().stream()
                .findFirst().map(a -> a.getAuthority().replace("ROLE_", "")).orElse("");
        String cible = switch (role) {
            case "STAGIAIRE" -> "stagiaire";
            case "ENCADRANT" -> "encadrant";
            default -> body.getOrDefault("cible", "stagiaire");
        };
        return ResponseEntity.ok(
                conventionServiceExtended.signerElectronique(
                        id, body.get("signature"), cible, userDetails.getUsername()
                )
        );
    }

    @GetMapping("/{id}/signature-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getSignatureStatus(@PathVariable Long id) {
        return ResponseEntity.ok(conventionServiceExtended.getSignatureStatus(id));
    }

    // ════════════════════════════════════════
    // PDF
    // ════════════════════════════════════════
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT','STAGIAIRE')")
    public ResponseEntity<byte[]> getPdf(@PathVariable Long id) {
        Convention c = conventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Convention introuvable"));

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
                        "attachment; filename=\"convocation-" + c.getNumero() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ════════════════════════════════════════
    // SUPPRESSION
    // ════════════════════════════════════════
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<?> supprimer(@PathVariable Long id) {
        conventionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}