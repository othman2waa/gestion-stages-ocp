package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Service.interfaces.DepartementService;
import com.OCP.Gestion_Stages.Service.interfaces.EncadrantService;
import com.OCP.Gestion_Stages.Service.interfaces.StagiaireService;
import com.OCP.Gestion_Stages.domain.dto.departement.DepartementRequest;
import com.OCP.Gestion_Stages.domain.dto.departement.DepartementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/departements")
@RequiredArgsConstructor
public class DepartementController {

    private final DepartementService departementService;
    private final StagiaireService stagiaireService;
    private final EncadrantService encadrantService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<List<DepartementResponse>> getAll() {
        return ResponseEntity.ok(departementService.findAll());
    }

    @GetMapping("/actifs")
    public ResponseEntity<List<DepartementResponse>> getActifs() {
        return ResponseEntity.ok(departementService.findActifs());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<DepartementResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(departementService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<DepartementResponse> create(@RequestBody DepartementRequest request) {
        return ResponseEntity.ok(departementService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<DepartementResponse> update(@PathVariable Long id,
                                                      @RequestBody DepartementRequest request) {
        return ResponseEntity.ok(departementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        departementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<Void> toggle(@PathVariable Long id) {
        departementService.toggleActif(id);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/{id}/stagiaires")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<?>> getStagiaires(@PathVariable Long id) {
        return ResponseEntity.ok(stagiaireService.findByDepartement(id));
    }

    @GetMapping("/{id}/encadrants")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<?>> getEncadrants(@PathVariable Long id) {
        return ResponseEntity.ok(encadrantService.findByDepartement(id));
    }
}