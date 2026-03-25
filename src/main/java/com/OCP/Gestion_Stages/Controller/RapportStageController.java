package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Service.interfaces.RapportStageService;
import com.OCP.Gestion_Stages.domain.dto.rapport.RapportResponse;
import com.OCP.Gestion_Stages.domain.model.RapportStage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/rapports")
@RequiredArgsConstructor
public class RapportStageController {

    private final RapportStageService rapportService;

    @PostMapping("/stage/{stageId}")
    @PreAuthorize("hasAnyRole('STAGIAIRE','ADMIN_RH')")
    public ResponseEntity<RapportResponse> upload(
            @PathVariable Long stageId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        return ResponseEntity.ok(rapportService.upload(stageId, file, userDetails.getUsername()));
    }

    @GetMapping("/stage/{stageId}/download")
    @PreAuthorize("hasAnyRole('STAGIAIRE','ENCADRANT','ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<byte[]> download(@PathVariable Long stageId) {
        RapportStage rapport = rapportService.download(stageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(rapport.getTypeContenu()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + rapport.getNomFichier() + "\"")
                .body(rapport.getContenu());
    }

    @GetMapping("/stage/{stageId}")
    @PreAuthorize("hasAnyRole('STAGIAIRE','ENCADRANT','ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<RapportResponse> getMeta(@PathVariable Long stageId) {
        return ResponseEntity.ok(rapportService.getMeta(stageId));
    }

    @DeleteMapping("/stage/{stageId}")
    @PreAuthorize("hasAnyRole('STAGIAIRE','ADMIN_RH')")
    public ResponseEntity<Void> delete(@PathVariable Long stageId) {
        rapportService.delete(stageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mes-rapports")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<List<RapportResponse>> getMesRapports(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(rapportService.getMesRapports(userDetails.getUsername()));
    }
}