package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Repository.CandidatureRepository;
import com.OCP.Gestion_Stages.Service.interfaces.StagiaireService;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.StagiaireRequest;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.StagiaireResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.MonDashboardResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
import java.util.Map;
import com.OCP.Gestion_Stages.Repository.UserRepository;
import com.OCP.Gestion_Stages.Repository.StagiaireRepository;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;

@RestController
@RequestMapping("/api/stagiaires")
@RequiredArgsConstructor
public class StagiaireController {

    private final StagiaireService stagiaireService;
    private final CandidatureRepository candidatureRepository;
    private final UserRepository userRepository;
    private final StagiaireRepository stagiaireRepository;


    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<List<StagiaireResponse>> getAll() {
        return ResponseEntity.ok(stagiaireService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<StagiaireResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stagiaireService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<StagiaireResponse> create(@Valid @RequestBody StagiaireRequest request) {
        return ResponseEntity.ok(stagiaireService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<StagiaireResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody StagiaireRequest request) {
        return ResponseEntity.ok(stagiaireService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stagiaireService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<List<StagiaireResponse>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(stagiaireService.search(keyword));
    }

    @GetMapping("/rechercher")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<Page<StagiaireResponse>> rechercher(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String niveau,
            @RequestParam(required = false) String filiere,
            @RequestParam(required = false) Long departementId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "nom") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Pageable pageable = PageRequest.of(page, size,
                sortDir.equals("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());
        return ResponseEntity.ok(stagiaireService.rechercher(keyword, niveau, filiere, departementId, pageable));
    }
    @GetMapping("/mon-dashboard")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<MonDashboardResponse> getMonDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(stagiaireService.getMonDashboard(userDetails.getUsername()));
    }

    @PatchMapping("/{id}/activer")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<Void> activerCompte(@PathVariable Long id) {
        stagiaireService.activerCompte(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<Void> desactiverCompte(@PathVariable Long id) {
        stagiaireService.desactiverCompte(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable Long id) {
        String newPassword = stagiaireService.resetPassword(id);
        return ResponseEntity.ok(Map.of("password", newPassword));
    }

    @GetMapping("/comptes")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<List<StagiaireResponse>> getAllAvecComptes() {
        return ResponseEntity.ok(stagiaireService.findAllAvecComptes());
    }

    @GetMapping("/mes-stagiaires")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<List<StagiaireResponse>> getMesStagiaires(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(stagiaireService.getMesStagiaires(userDetails.getUsername()));
    }


    @PatchMapping("/ma-fiche")
    @PreAuthorize("hasRole('STAGIAIRE')")
    @Transactional
    public ResponseEntity<?> saveFiche(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User introuvable"));
        var stagiaire = stagiaireRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire introuvable"));

        if (body.get("adressePersonnelle") != null)
            stagiaire.setAdressePersonnelle((String) body.get("adressePersonnelle"));
        if (body.get("villeEtablissement") != null)
            stagiaire.setVilleEtablissement((String) body.get("villeEtablissement"));
        if (body.get("adresseLogementStage") != null)
            stagiaire.setAdresseLogementStage((String) body.get("adresseLogementStage"));
        if (body.get("contactUrgenceNom") != null)
            stagiaire.setContactUrgenceNom((String) body.get("contactUrgenceNom"));
        if (body.get("contactUrgenceTel") != null)
            stagiaire.setContactUrgenceTel((String) body.get("contactUrgenceTel"));
        if (body.get("serviceAccueil") != null)
            stagiaire.setServiceAccueil((String) body.get("serviceAccueil"));
        if (body.get("diplome") != null)
            stagiaire.setDiplome((String) body.get("diplome"));
        if (body.get("observation") != null)
            stagiaire.setObservation((String) body.get("observation"));
        if (body.get("reglesAcceptees") != null)
            stagiaire.setReglesAcceptees((Boolean) body.get("reglesAcceptees"));

        stagiaire.setFicheRenseignementCompletee(true);
        stagiaireRepository.save(stagiaire);

        return ResponseEntity.ok(Map.of(
                "message", "Fiche enregistrée avec succès",
                "ficheCompletee", true
        ));
    }


    @GetMapping("/mon-profil")
    @Transactional
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<?> getMonProfil(
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User introuvable"));
        var stagiaire = stagiaireRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire introuvable"));

        Map<String, Object> profil = new java.util.LinkedHashMap<>();
        profil.put("id", stagiaire.getId());
        profil.put("nom", stagiaire.getNom());
        profil.put("prenom", stagiaire.getPrenom());
        profil.put("email", stagiaire.getEmail());
        profil.put("filiere", stagiaire.getFiliere());
        profil.put("niveau", stagiaire.getNiveau());

        // Candidature complète
        candidatureRepository.findAll().stream()
                .filter(c -> c.getEmail().equals(stagiaire.getEmail()))
                .findFirst()
                .ifPresent(c -> {
                    profil.put("candidatureId", c.getId());
                    profil.put("candidatureStatut", c.getStatut());
                    profil.put("candidatureStatutMeeting", c.getStatutMeeting());
                    profil.put("candidatureDateMeeting", c.getDateMeeting() != null ? c.getDateMeeting().toString() : null);
                    profil.put("candidatureSpecialite", c.getSpecialite());
                    profil.put("candidatureDepartement", c.getDepartement() != null ? c.getDepartement().getNom() : "");
                    profil.put("candidatureCreatedAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
                    profil.put("convocationEnvoyee", c.getConvocationEnvoyee() != null ? c.getConvocationEnvoyee() : false);
                    profil.put("dateConvocation", c.getDateConvocation() != null ? c.getDateConvocation().toString() : null);
                    // Nb documents uploadés
                    long nbDocs = candidatureRepository.findById(c.getId())
                            .map(cand -> cand.getDocuments() != null ? cand.getDocuments().size() : 0)
                            .orElse(0).longValue();
                    profil.put("nbDocuments", nbDocs);
                });

        return ResponseEntity.ok(profil);
    }


}