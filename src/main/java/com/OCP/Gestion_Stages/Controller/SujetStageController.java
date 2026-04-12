package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.domain.model.*;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;




@RestController
@RequestMapping("/api/sujets")
@RequiredArgsConstructor
@Transactional
public class SujetStageController {

    private final SujetStageRepository sujetRepository;
    private final EncadrantRepository encadrantRepository;
    private final DepartementRepository departementRepository;
    private final StageRepository stageRepository;
    private final UserRepository userRepository;

    // ── Encadrant propose un sujet
    @PostMapping
    @PreAuthorize("hasAnyRole('ENCADRANT','ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<?> proposer(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        SujetStage sujet = new SujetStage();
        sujet.setTitre((String) body.get("titre"));
        sujet.setDescription((String) body.get("description"));
        sujet.setTechnologies((String) body.get("technologies"));
        sujet.setNiveauRequis((String) body.get("niveauRequis"));
        sujet.setTypeStage((String) body.get("typeStage"));
        sujet.setStatut("PROPOSE");

        if (body.get("encadrantId") != null) {
            Long encId = Long.valueOf(body.get("encadrantId").toString());
            encadrantRepository.findById(encId).ifPresent(sujet::setEncadrant);
        } else {
            // Auto-assign si encadrant connecté
            var user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
            if (user != null) {
                encadrantRepository.findAll().stream()
                        .filter(e -> e.getUser() != null && e.getUser().getId().equals(user.getId()))
                        .findFirst().ifPresent(sujet::setEncadrant);
            }
        }

        if (body.get("departementId") != null) {
            Long deptId = Long.valueOf(body.get("departementId").toString());
            departementRepository.findById(deptId).ifPresent(sujet::setDepartement);
        }

        return ResponseEntity.ok(toResponse(sujetRepository.save(sujet)));
    }

    // ── Admin voit tous les sujets
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<?>> getAll() {
        return ResponseEntity.ok(
                sujetRepository.findAllByOrderByCreatedAtDesc()
                        .stream().map(this::toResponse).collect(Collectors.toList())
        );
    }

    // ── Admin voit sujets par statut
    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<?>> getByStatut(@PathVariable String statut) {
        return ResponseEntity.ok(
                sujetRepository.findByStatutOrderByCreatedAtDesc(statut)
                        .stream().map(this::toResponse).collect(Collectors.toList())
        );
    }

    // ── Encadrant voit ses sujets
    @GetMapping("/mes-sujets")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<List<?>> getMesSujets(
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User introuvable"));
        var encadrant = encadrantRepository.findAll().stream()
                .filter(e -> e.getUser() != null && e.getUser().getId().equals(user.getId()))
                .findFirst().orElseThrow(() -> new ResourceNotFoundException("Encadrant introuvable"));
        return ResponseEntity.ok(
                sujetRepository.findByEncadrantIdOrderByCreatedAtDesc(encadrant.getId())
                        .stream().map(this::toResponse).collect(Collectors.toList())
        );
    }

    // ── Admin valide un sujet
    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<?> valider(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        SujetStage sujet = sujetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sujet introuvable"));
        sujet.setStatut("VALIDE");
        sujet.setValidatedAt(LocalDateTime.now());
        sujet.setValidatedBy(userDetails.getUsername());
        return ResponseEntity.ok(toResponse(sujetRepository.save(sujet)));
    }

    // ── Admin refuse un sujet
    @PatchMapping("/{id}/refuser")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<?> refuser(@PathVariable Long id) {
        SujetStage sujet = sujetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sujet introuvable"));
        sujet.setStatut("REFUSE");
        return ResponseEntity.ok(toResponse(sujetRepository.save(sujet)));
    }

    // ── Admin affecte sujet + encadrant à un stage
    @PatchMapping("/{id}/affecter")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<?> affecter(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        SujetStage sujet = sujetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sujet introuvable"));

        Long stageId = Long.valueOf(body.get("stageId").toString());
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable"));

        // Affecter le sujet au stage
        stage.setSujet(sujet.getTitre());
        stage.setSujetValide(true);
        stage.setSujetProposePar(sujet.getEncadrant() != null
                ? sujet.getEncadrant().getPrenom() + " " + sujet.getEncadrant().getNom()
                : userDetails.getUsername());

        // Affecter l'encadrant si spécifié
        if (body.get("encadrantId") != null) {
            Long encId = Long.valueOf(body.get("encadrantId").toString());
            encadrantRepository.findById(encId).ifPresent(stage::setEncadrant);
        } else if (sujet.getEncadrant() != null) {
            stage.setEncadrant(sujet.getEncadrant());
        }

        stageRepository.save(stage);
        sujet.setStatut("AFFECTE");
        sujet.setStage(stage);
        return ResponseEntity.ok(toResponse(sujetRepository.save(sujet)));
    }

    // ── Supprimer un sujet
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<?> supprimer(@PathVariable Long id) {
        sujetRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    private Map<String, Object> toResponse(SujetStage s) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("titre", s.getTitre());
        map.put("description", s.getDescription() != null ? s.getDescription() : "");
        map.put("technologies", s.getTechnologies() != null ? s.getTechnologies() : "");
        map.put("niveauRequis", s.getNiveauRequis() != null ? s.getNiveauRequis() : "");
        map.put("typeStage", s.getTypeStage() != null ? s.getTypeStage() : "");
        map.put("statut", s.getStatut());
        map.put("encadrantNom", s.getEncadrant() != null
                ? s.getEncadrant().getPrenom() + " " + s.getEncadrant().getNom() : "");
        map.put("encadrantId", s.getEncadrant() != null ? s.getEncadrant().getId() : null);
        map.put("departementNom", s.getDepartement() != null ? s.getDepartement().getNom() : "");
        map.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : "");
        map.put("validatedBy", s.getValidatedBy() != null ? s.getValidatedBy() : "");
        map.put("stageId", s.getStage() != null ? s.getStage().getId() : null);
        return map;
    }
}