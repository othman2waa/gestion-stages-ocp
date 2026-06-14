package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Service.interfaces.WorkflowService;
import com.OCP.Gestion_Stages.domain.dto.StageHistoriqueResponse;
import com.OCP.Gestion_Stages.domain.dto.WorkflowRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping("/stages/{id}/transition")
    @PreAuthorize("hasAnyRole('ADMIN_RH', 'RESPONSABLE_RH', 'ENCADRANT')")
    public ResponseEntity<Void> transitionner(
            @PathVariable Long id,
            @RequestBody WorkflowRequest request) {
        workflowService.transitionner(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stages/{id}/historique")
    @PreAuthorize("hasAnyRole('ADMIN_RH', 'RESPONSABLE_RH', 'ENCADRANT', 'STAGIAIRE')")
    public ResponseEntity<List<StageHistoriqueResponse>> getHistorique(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getHistorique(id));
    }

    @GetMapping("/stages/{id}/transitions-possibles")
    @PreAuthorize("hasAnyRole('ADMIN_RH', 'RESPONSABLE_RH', 'ENCADRANT')")
    public ResponseEntity<List<String>> getTransitionsPossibles(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getTransitionsPossibles(id));
    }
}