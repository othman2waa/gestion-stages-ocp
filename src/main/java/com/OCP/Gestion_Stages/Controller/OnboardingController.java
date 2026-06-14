package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Repository.OnboardingChecklistRepository;
import com.OCP.Gestion_Stages.Repository.StagiaireRepository;
import com.OCP.Gestion_Stages.domain.dto.onboarding.ChecklistItemResponse;
import com.OCP.Gestion_Stages.domain.model.OnboardingChecklist;
import com.OCP.Gestion_Stages.domain.model.Stagiaire;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.OCP.Gestion_Stages.Repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingChecklistRepository checklistRepository;
    private final StagiaireRepository stagiaireRepository;
    private final UserRepository userRepository;

    @GetMapping("/stagiaire/{stagiaireId}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<List<ChecklistItemResponse>> getChecklist(@PathVariable Long stagiaireId) {
        return ResponseEntity.ok(
                checklistRepository.findByStagiaireIdOrderByOrdreAsc(stagiaireId)
                        .stream().map(this::toResponse).collect(Collectors.toList())
        );
    }

    @GetMapping("/mon-checklist")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<List<ChecklistItemResponse>> getMonChecklist(
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        var stagiaire = stagiaireRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire introuvable"));
        return ResponseEntity.ok(
                checklistRepository.findByStagiaireIdOrderByOrdreAsc(stagiaire.getId())
                        .stream().map(this::toResponse).collect(Collectors.toList())
        );
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<ChecklistItemResponse> completer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        OnboardingChecklist item = checklistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item introuvable"));
        item.setCompleted(!Boolean.TRUE.equals(item.getCompleted()));
        item.setCompletedAt(item.getCompleted() ? LocalDateTime.now() : null);
        item.setCompletedBy(item.getCompleted() ? userDetails.getUsername() : null);
        return ResponseEntity.ok(toResponse(checklistRepository.save(item)));
    }

    @GetMapping("/stats/{stagiaireId}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT','STAGIAIRE')")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable Long stagiaireId) {
        long total = checklistRepository.countByStagiaireId(stagiaireId);
        long completed = checklistRepository.countByStagiaireIdAndCompletedTrue(stagiaireId);
        int progression = total > 0 ? (int) (completed * 100 / total) : 0;
        return ResponseEntity.ok(Map.of(
                "total", total,
                "completed", completed,
                "remaining", total - completed,
                "progression", progression
        ));
    }

    private ChecklistItemResponse toResponse(OnboardingChecklist c) {
        ChecklistItemResponse r = new ChecklistItemResponse();
        r.setId(c.getId());
        r.setEtape(c.getEtape());
        r.setCategorie(c.getCategorie());
        r.setDescription(c.getDescription());
        r.setCompleted(c.getCompleted());
        r.setCompletedAt(c.getCompletedAt());
        r.setCompletedBy(c.getCompletedBy());
        r.setOrdre(c.getOrdre());
        if (c.getStagiaire() != null) {
            r.setStagiaireId(c.getStagiaire().getId());
            r.setStagiaireNom(c.getStagiaire().getPrenom() + " " + c.getStagiaire().getNom());
        }
        return r;
    }
}