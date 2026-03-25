package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Service.interfaces.CandidatureService;
import com.OCP.Gestion_Stages.domain.dto.candidature.CandidatureRequest;
import com.OCP.Gestion_Stages.domain.dto.candidature.CandidatureResponse;
import com.OCP.Gestion_Stages.domain.dto.candidature.TraiterCandidatureRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/candidatures")
@RequiredArgsConstructor
public class CandidatureController {

    private final CandidatureService candidatureService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CandidatureResponse> soumettre(
            @RequestPart("data") CandidatureRequest request,
            @RequestPart(value = "cv", required = false) MultipartFile cv) throws Exception {
        return ResponseEntity.ok(candidatureService.soumettre(request, cv));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<CandidatureResponse>> getAll() {
        return ResponseEntity.ok(candidatureService.findAll());
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<CandidatureResponse>> getByStatut(@PathVariable String statut) {
        return ResponseEntity.ok(candidatureService.findByStatut(statut));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<CandidatureResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(candidatureService.findById(id));
    }

    @PatchMapping("/{id}/traiter")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<CandidatureResponse> traiter(
            @PathVariable Long id,
            @RequestBody TraiterCandidatureRequest request,
            @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        return ResponseEntity.ok(candidatureService.traiter(id, request, userDetails.getUsername()));
    }

    @GetMapping("/{id}/cv")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<byte[]> getCv(@PathVariable Long id) {
        byte[] cv = candidatureService.getCv(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=cv.pdf")
                .body(cv);
    }
}