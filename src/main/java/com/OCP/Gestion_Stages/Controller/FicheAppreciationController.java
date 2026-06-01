package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Service.interfaces.FicheAppreciationService;
import com.OCP.Gestion_Stages.domain.dto.fiche.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fiches-appreciation")
@RequiredArgsConstructor
public class FicheAppreciationController {

    private final FicheAppreciationService ficheService;

    // ── Fiche Stage ──

    @PostMapping("/stage")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<FicheStageResponse> createFicheStage(
            @Valid @RequestBody FicheStageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ficheService.createFicheStage(request, userDetails.getUsername()));
    }

    @PutMapping("/stage/{id}")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<FicheStageResponse> updateFicheStage(
            @PathVariable Long id,
            @Valid @RequestBody FicheStageRequest request) {
        return ResponseEntity.ok(ficheService.updateFicheStage(id, request));
    }

    @GetMapping("/stage/by-stage/{stageId}")
    @PreAuthorize("hasAnyRole('ENCADRANT','ADMIN_RH','RESPONSABLE_RH','STAGIAIRE')")
    public ResponseEntity<FicheStageResponse> getFicheStageByStage(@PathVariable Long stageId) {
        return ResponseEntity.ok(ficheService.getFicheStageByStageId(stageId));
    }

    @GetMapping("/stage")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<FicheStageResponse>> getAllFichesStage() {
        return ResponseEntity.ok(ficheService.getAllFichesStage());
    }

    @GetMapping("/encadrant/mes-fiches-stage")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<List<FicheStageResponse>> getMesFichesStage(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ficheService.getMesFichesStage(userDetails.getUsername()));
    }

    // ── Fiche Stagiaire ──

    @PostMapping("/stagiaire")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<FicheStagiaireResponse> createFicheStagiaire(
            @Valid @RequestBody FicheStagiaireRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ficheService.createFicheStagiaire(request, userDetails.getUsername()));
    }

    @PutMapping("/stagiaire/{id}")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<FicheStagiaireResponse> updateFicheStagiaire(
            @PathVariable Long id,
            @Valid @RequestBody FicheStagiaireRequest request) {
        return ResponseEntity.ok(ficheService.updateFicheStagiaire(id, request));
    }

    @GetMapping("/stagiaire/by-stage/{stageId}")
    @PreAuthorize("hasAnyRole('ENCADRANT','ADMIN_RH','RESPONSABLE_RH','STAGIAIRE')")
    public ResponseEntity<FicheStagiaireResponse> getFicheStagiaireByStage(@PathVariable Long stageId) {
        return ResponseEntity.ok(ficheService.getFicheStagiaireByStageId(stageId));
    }

    @GetMapping("/stagiaire")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<FicheStagiaireResponse>> getAllFichesStagiaire() {
        return ResponseEntity.ok(ficheService.getAllFichesStagiaire());
    }

    @GetMapping("/encadrant/mes-fiches-stagiaire")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<List<FicheStagiaireResponse>> getMesFichesStagiaire(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ficheService.getMesFichesStagiaire(userDetails.getUsername()));
    }
}
