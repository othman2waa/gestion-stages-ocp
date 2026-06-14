package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.Service.ArchiveStageService;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.model.Stage;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
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
@RequestMapping("/api/archives")
@RequiredArgsConstructor
@Transactional
public class ArchiveController {

    private final ArchiveStageService archiveService;
    private final StageRepository stageRepository;

    // ── GET toutes les archives avec filtres
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) String departement) {
        return ResponseEntity.ok(archiveService.getAll(annee, departement));
    }

    // ── GET statistiques
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(archiveService.getStats());
    }

    // ── POST purge de rétention : anonymise les archives expirées + supprime leurs documents
    @PostMapping("/purger")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<Map<String, Object>> purger(@AuthenticationPrincipal UserDetails userDetails) {
        int n = archiveService.purgerArchivesExpirees(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("anonymises", n,
                "message", n + " archive(s) expirée(s) anonymisée(s) ; documents supprimés."));
    }

    // ── POST archiver manuellement un stage
    @PostMapping("/archiver/{stageId}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<?> archiver(
            @PathVariable Long stageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable"));
        var archive = archiveService.archiverStage(stage, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "message", "Stage archivé avec succès",
                "archiveId", archive.getId()
        ));
    }

    // ── POST archiver tous les stages TERMINE
    @PostMapping("/archiver-tous")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<?> archiverTous(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<Stage> termines = stageRepository.findByStatut(StageStatus.TERMINE);
        int count = 0;
        for (Stage stage : termines) {
            try {
                archiveService.archiverStage(stage, userDetails.getUsername());
                count++;
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(Map.of(
                "message", count + " stage(s) archivé(s)",
                "total", count
        ));
    }

    // ── GET filtres disponibles
    @GetMapping("/filtres")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<?> getFiltres() {
        return ResponseEntity.ok(Map.of(
                "annees", archiveService.getStats().get("annees"),
                "departements", archiveService.getStats().get("departements")
        ));
    }
}