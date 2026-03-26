package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Service.interfaces.StagiaireService;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.StagiaireRequest;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.StagiaireResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.MonDashboardResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/stagiaires")
@RequiredArgsConstructor
public class StagiaireController {

    private final StagiaireService stagiaireService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<List<StagiaireResponse>> getAll() {
        return ResponseEntity.ok(stagiaireService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<StagiaireResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stagiaireService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<StagiaireResponse> create(@Valid @RequestBody StagiaireRequest request) {
        return ResponseEntity.ok(stagiaireService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<StagiaireResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody StagiaireRequest request) {
        return ResponseEntity.ok(stagiaireService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stagiaireService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<List<StagiaireResponse>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(stagiaireService.search(keyword));
    }
    @GetMapping("/mon-dashboard")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<MonDashboardResponse> getMonDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(stagiaireService.getMonDashboard(userDetails.getUsername()));
    }

    @PatchMapping("/{id}/activer")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<Void> activerCompte(@PathVariable Long id) {
        stagiaireService.activerCompte(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<Void> desactiverCompte(@PathVariable Long id) {
        stagiaireService.desactiverCompte(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable Long id) {
        String newPassword = stagiaireService.resetPassword(id);
        return ResponseEntity.ok(Map.of("password", newPassword));
    }

    @GetMapping("/comptes")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<List<StagiaireResponse>> getAllAvecComptes() {
        return ResponseEntity.ok(stagiaireService.findAllAvecComptes());
    }

}