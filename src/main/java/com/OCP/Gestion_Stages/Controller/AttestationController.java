package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Repository.AttestationStageRepository;
import com.OCP.Gestion_Stages.Service.AttestationPdfService;
import com.OCP.Gestion_Stages.Service.interfaces.AttestationServiceExtended;
import com.OCP.Gestion_Stages.domain.dto.attestation.AttestationDTO;
import com.OCP.Gestion_Stages.domain.model.AttestationStage;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import com.OCP.Gestion_Stages.exeptions.BusinessException;
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

@Transactional
@RestController
@RequestMapping("/api/attestations")
@RequiredArgsConstructor
public class AttestationController {

    // ── Services (logique déléguée)
    private final AttestationServiceExtended attestationService;
    private final AttestationPdfService pdfService;

    // ── Repository (uniquement pour getPdf)
    private final AttestationStageRepository attestationRepository;

    @PostMapping("/demander")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<?> demander(
            @RequestBody Map<String, Long> body) {
        try {
            return ResponseEntity.ok(attestationService.demander(body.get("stageId")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/ma-demande/{stageId}")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<AttestationDTO> getMaDemande(@PathVariable Long stageId) {
        return ResponseEntity.ok(attestationService.getMaDemande(stageId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<AttestationDTO>> getAll() {
        return ResponseEntity.ok(attestationService.getAll());
    }

    @GetMapping("/en-attente")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<AttestationDTO>> getEnAttente() {
        return ResponseEntity.ok(attestationService.getEnAttente());
    }

    @PatchMapping("/{id}/approuver")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<AttestationDTO> approuver(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                attestationService.approuver(id, userDetails.getUsername())
        );
    }

    @PatchMapping("/{id}/refuser")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<AttestationDTO> refuser(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                attestationService.refuser(id, body.get("commentaire"), userDetails.getUsername())
        );
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','STAGIAIRE')")
    public ResponseEntity<byte[]> getPdf(@PathVariable Long id) {
        AttestationStage att = attestationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attestation introuvable"));
        if (!"APPROUVEE".equals(att.getStatut()))
            throw new BusinessException("Attestation non approuvée : le PDF n'est disponible qu'après approbation.");
        byte[] pdf = pdfService.genererAttestation(att);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"attestation-" + att.getNumeroAttestation() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/demande-pdf")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','STAGIAIRE')")
    public ResponseEntity<byte[]> getDemandePdf(@PathVariable Long id) {
        AttestationStage att = attestationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attestation introuvable"));
        byte[] pdf = pdfService.genererDemande(att);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"demande-attestation-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}