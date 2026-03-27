package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Service.interfaces.AnnonceService;
import com.OCP.Gestion_Stages.domain.dto.annonce.AnnonceRequest;
import com.OCP.Gestion_Stages.domain.dto.annonce.AnnonceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/annonces")
@RequiredArgsConstructor
public class AnnonceController {

    private final AnnonceService annonceService;

    @GetMapping("/publiques")
    public ResponseEntity<List<AnnonceResponse>> getActives() {
        return ResponseEntity.ok(annonceService.findActives());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<AnnonceResponse>> getAll() {
        return ResponseEntity.ok(annonceService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnnonceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(annonceService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<AnnonceResponse> create(
            @RequestBody AnnonceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(annonceService.create(request, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<AnnonceResponse> update(@PathVariable Long id,
                                                  @RequestBody AnnonceRequest request) {
        return ResponseEntity.ok(annonceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        annonceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<Void> toggle(@PathVariable Long id) {
        annonceService.toggleActif(id);
        return ResponseEntity.ok().build();
    }
}