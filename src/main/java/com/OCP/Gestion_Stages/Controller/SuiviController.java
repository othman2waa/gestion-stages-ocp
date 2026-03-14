package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Service.interfaces.SuiviService;
import com.OCP.Gestion_Stages.domain.dto.suivi.SuiviRequest;
import com.OCP.Gestion_Stages.domain.dto.suivi.SuiviResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/suivis")
@RequiredArgsConstructor
public class SuiviController {

    private final SuiviService suiviService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ENCADRANT','ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<SuiviResponse> create(
            @RequestBody SuiviRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(suiviService.create(request, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ENCADRANT','ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<SuiviResponse> update(@PathVariable Long id,
                                                @RequestBody SuiviRequest request) {
        return ResponseEntity.ok(suiviService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ENCADRANT','ADMIN_RH')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        suiviService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stage/{stageId}")
    @PreAuthorize("hasAnyRole('ENCADRANT','ADMIN_RH','RESPONSABLE_RH','STAGIAIRE')")
    public ResponseEntity<List<SuiviResponse>> getByStage(@PathVariable Long stageId) {
        return ResponseEntity.ok(suiviService.findByStage(stageId));
    }

    @GetMapping("/mes-suivis")
    @PreAuthorize("hasAnyRole('ENCADRANT','ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<SuiviResponse>> getMesSuivis(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(suiviService.findByEncadrant(userDetails.getUsername()));
    }
}