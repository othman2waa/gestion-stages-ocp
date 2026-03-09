package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Service.interfaces.StageService;
import com.OCP.Gestion_Stages.domain.dto.stage.StageRequest;
import com.OCP.Gestion_Stages.domain.dto.stage.StageResponse;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stages")
@RequiredArgsConstructor
public class StageController {

    private final StageService stageService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<List<StageResponse>> getAll() {
        return ResponseEntity.ok(stageService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<StageResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stageService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<StageResponse> create(@Valid @RequestBody StageRequest request) {
        return ResponseEntity.ok(stageService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<StageResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody StageRequest request) {
        return ResponseEntity.ok(stageService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stageService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<StageResponse> updateStatut(@PathVariable Long id,
                                                      @RequestParam StageStatus statut) {
        return ResponseEntity.ok(stageService.updateStatut(id, statut));
    }

    @GetMapping("/stagiaire/{stagiaireId}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT','STAGIAIRE')")
    public ResponseEntity<List<StageResponse>> getByStagiaire(@PathVariable Long stagiaireId) {
        return ResponseEntity.ok(stageService.findByStagiaire(stagiaireId));
    }

    @GetMapping("/encadrant/{encadrantId}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<List<StageResponse>> getByEncadrant(@PathVariable Long encadrantId) {
        return ResponseEntity.ok(stageService.findByEncadrant(encadrantId));
    }
    @GetMapping("/rechercher")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<Page<StageResponse>> rechercher(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) StageStatus statut,
            @RequestParam(required = false) String typeStage,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = PageRequest.of(page, size,
                sortDir.equals("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());

        return ResponseEntity.ok(stageService.rechercher(keyword, statut, typeStage, null, pageable));
    }
}