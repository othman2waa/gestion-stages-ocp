package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Repository.AttestationStageRepository;
import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.Repository.UserRepository;
import com.OCP.Gestion_Stages.Repository.StagiaireRepository;
import com.OCP.Gestion_Stages.Service.AttestationPdfService;
import com.OCP.Gestion_Stages.domain.model.AttestationStage;
import com.OCP.Gestion_Stages.domain.model.Stage;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;import org.springframework.transaction.annotation.Transactional;




@Transactional
@RestController
@RequestMapping("/api/attestations")
@RequiredArgsConstructor
public class AttestationController {

    private final AttestationStageRepository attestationRepository;
    private final StageRepository stageRepository;
    private final UserRepository userRepository;
    private final StagiaireRepository stagiaireRepository;
    private final AttestationPdfService pdfService;

    // Stagiaire demande une attestation
    @PostMapping("/demander")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<?> demanderAttestation(
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long stageId = body.get("stageId");
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable"));

        // Vérifie si déjà demandée
        if (attestationRepository.findByStageId(stageId).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Attestation déjà demandée"));
        }

        AttestationStage att = new AttestationStage();
        att.setStage(stage);
        att.setStatut("EN_ATTENTE");
        return ResponseEntity.ok(toResponse(attestationRepository.save(att)));
    }

    // Stagiaire voit sa demande
    @GetMapping("/ma-demande/{stageId}")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<?> getMaDemande(@PathVariable Long stageId) {
        return attestationRepository.findByStageId(stageId)
                .map(att -> ResponseEntity.ok(toResponse(att)))
                .orElse(ResponseEntity.ok(null));
    }

    // Admin voit toutes les demandes
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<?>> getAll() {
        return ResponseEntity.ok(
                attestationRepository.findAllByOrderByDateDemandeDesc()
                        .stream().map(this::toResponse).toList()
        );
    }

    // Admin voit les demandes en attente
    @GetMapping("/en-attente")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<?>> getEnAttente() {
        return ResponseEntity.ok(
                attestationRepository.findByStatutOrderByDateDemandeDesc("EN_ATTENTE")
                        .stream().map(this::toResponse).toList()
        );
    }

    // Admin approuve et génère PDF
    @PatchMapping("/{id}/approuver")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<?> approuver(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        AttestationStage att = attestationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attestation introuvable"));
        att.setStatut("APPROUVEE");
        att.setDateTraitement(LocalDateTime.now());
        att.setTraitePar(userDetails.getUsername());
        // Générer numéro attestation
        String numero = "ATT-" + att.getStage().getId() + "-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        att.setNumeroAttestation(numero);
        return ResponseEntity.ok(toResponse(attestationRepository.save(att)));
    }

    // Admin refuse
    @PatchMapping("/{id}/refuser")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<?> refuser(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        AttestationStage att = attestationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attestation introuvable"));
        att.setStatut("REFUSEE");
        att.setDateTraitement(LocalDateTime.now());
        att.setTraitePar(userDetails.getUsername());
        att.setCommentaire(body.get("commentaire"));
        return ResponseEntity.ok(toResponse(attestationRepository.save(att)));
    }

    // Télécharger PDF attestation
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','STAGIAIRE')")
    public ResponseEntity<byte[]> getPdf(@PathVariable Long id) {
        AttestationStage att = attestationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attestation introuvable"));
        if (!"APPROUVEE".equals(att.getStatut())) {
            throw new RuntimeException("L'attestation n'est pas encore approuvée");
        }
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

    private Map<String, Object> toResponse(AttestationStage att) {
        Stage s = att.getStage();
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", att.getId());
        map.put("statut", att.getStatut());
        map.put("dateDemande", att.getDateDemande() != null ? att.getDateDemande().toString() : "");
        map.put("dateTraitement", att.getDateTraitement() != null ? att.getDateTraitement().toString() : "");
        map.put("traitePar", att.getTraitePar() != null ? att.getTraitePar() : "");
        map.put("numeroAttestation", att.getNumeroAttestation() != null ? att.getNumeroAttestation() : "");
        map.put("commentaire", att.getCommentaire() != null ? att.getCommentaire() : "");
        map.put("stageId", s != null ? s.getId() : null);
        map.put("stageSujet", s != null && s.getSujet() != null ? s.getSujet() : "");
        map.put("stagiaireNom", s != null && s.getStagiaire() != null
                ? s.getStagiaire().getPrenom() + " " + s.getStagiaire().getNom() : "");
        return map;
    }

}