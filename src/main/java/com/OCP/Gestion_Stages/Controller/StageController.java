package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Repository.UserRepository;
import com.OCP.Gestion_Stages.Service.interfaces.StageService;
import com.OCP.Gestion_Stages.domain.dto.stage.StageRequest;
import com.OCP.Gestion_Stages.domain.dto.stage.StageResponse;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.model.Encadrant;
import com.OCP.Gestion_Stages.domain.model.User;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.OCP.Gestion_Stages.Repository.EncadrantRepository;
import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.Service.LettreAccordPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/stages")
@RequiredArgsConstructor
public class StageController {

    private final StageService stageService;
    private final UserRepository userRepository;
    private final EncadrantRepository encadrantRepository;
    private final StageRepository stageRepository;
    private final LettreAccordPdfService lettreAccordPdfService;
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
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) StageStatus statut,
            @RequestParam(required = false) String typeStage,
            @RequestParam(required = false) Long departementId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = PageRequest.of(page, size,
                sortDir.equals("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());

        // Si l'utilisateur est ENCADRANT, filtrer uniquement ses stages
        Long encadrantId = null;
        boolean isEncadrant = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ENCADRANT"));
        if (isEncadrant) {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
            Encadrant encadrant = encadrantRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Profil encadrant introuvable"));
            encadrantId = encadrant.getId();
        }

        return ResponseEntity.ok(stageService.rechercher(keyword, statut, typeStage, departementId, encadrantId, pageable));
    }

    @GetMapping("/mes-stages")
    @PreAuthorize("hasAnyRole('ENCADRANT','ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<StageResponse>> getMesStages(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Encadrant encadrant = encadrantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profil encadrant introuvable"));
        return ResponseEntity.ok(stageService.findByEncadrant(encadrant.getId()));
    }

    // ── Lettre d'accord PFE (PDF)
    @GetMapping("/{id}/lettre-accord")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<byte[]> getLettreAccord(@PathVariable Long id) {
        var stage = stageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable"));
        byte[] pdf = lettreAccordPdfService.genererLettreAccord(stage);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=lettre-accord-" + id + ".pdf")
                .body(pdf);
    }
}