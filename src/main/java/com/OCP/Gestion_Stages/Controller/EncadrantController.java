package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Service.interfaces.EncadrantService;
import com.OCP.Gestion_Stages.domain.dto.encadrant.EncadrantRequest;
import com.OCP.Gestion_Stages.domain.dto.encadrant.EncadrantResponse;
import com.OCP.Gestion_Stages.domain.dto.encadrant.MonProfilEncadrantResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/encadrants")
@RequiredArgsConstructor
public class EncadrantController {

    private final EncadrantService encadrantService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<EncadrantResponse>> getAll() {
        return ResponseEntity.ok(encadrantService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<EncadrantResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(encadrantService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<EncadrantResponse> create(@Valid @RequestBody EncadrantRequest request) {
        return ResponseEntity.ok(encadrantService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<EncadrantResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody EncadrantRequest request) {
        return ResponseEntity.ok(encadrantService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        encadrantService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/departement/{departementId}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<EncadrantResponse>> getByDepartement(@PathVariable Long departementId) {
        return ResponseEntity.ok(encadrantService.findByDepartement(departementId));
    }

    @GetMapping("/mon-profil")
    @PreAuthorize("hasAnyRole('ENCADRANT','ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<MonProfilEncadrantResponse> getMonProfil(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(encadrantService.getMonProfil(userDetails.getUsername()));
    }
}