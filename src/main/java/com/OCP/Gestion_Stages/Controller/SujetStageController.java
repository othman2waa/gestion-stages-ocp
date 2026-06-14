package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Repository.SujetStageRepository;
import com.OCP.Gestion_Stages.Service.interfaces.SujetStageServiceExtended;
import com.OCP.Gestion_Stages.domain.dto.sujet.SujetStageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sujets")
@RequiredArgsConstructor
@Transactional
public class SujetStageController {

    // ── Service (toute la logique déléguée)
    private final SujetStageServiceExtended sujetService;

    // ── Repository (uniquement pour suppression simple)
    private final SujetStageRepository sujetRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ENCADRANT','ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<SujetStageDTO> proposer(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                sujetService.proposer(body, userDetails.getUsername())
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<SujetStageDTO>> getAll() {
        return ResponseEntity.ok(sujetService.getAll());
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<SujetStageDTO>> getByStatut(@PathVariable String statut) {
        return ResponseEntity.ok(sujetService.getByStatut(statut));
    }

    @GetMapping("/mes-sujets")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<List<SujetStageDTO>> getMesSujets(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                sujetService.getMesSujets(userDetails.getUsername())
        );
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<SujetStageDTO> valider(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                sujetService.valider(id, userDetails.getUsername())
        );
    }

    @PatchMapping("/{id}/refuser")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<SujetStageDTO> refuser(@PathVariable Long id) {
        return ResponseEntity.ok(sujetService.refuser(id));
    }

    @PatchMapping("/{id}/affecter")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<SujetStageDTO> affecter(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                sujetService.affecter(id, body, userDetails.getUsername())
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<?> supprimer(@PathVariable Long id) {
        sujetRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}