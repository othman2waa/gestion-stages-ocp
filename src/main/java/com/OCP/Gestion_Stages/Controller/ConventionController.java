package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Service.ConventionPdfService;
import com.OCP.Gestion_Stages.Service.interfaces.ConventionService;
import com.OCP.Gestion_Stages.domain.dto.convention.ConventionRequest;
import com.OCP.Gestion_Stages.domain.dto.convention.ConventionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conventions")
@RequiredArgsConstructor
public class ConventionController {

    private final ConventionService conventionService;
    private final ConventionPdfService conventionPdfService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<ConventionResponse>> getAll() {
        return ResponseEntity.ok(conventionService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<ConventionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(conventionService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<ConventionResponse> create(@Valid @RequestBody ConventionRequest request) {
        return ResponseEntity.ok(conventionService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<ConventionResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody ConventionRequest request) {
        return ResponseEntity.ok(conventionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        conventionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stage/{stageId}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<ConventionResponse> getByStage(@PathVariable Long stageId) {
        return ResponseEntity.ok(conventionService.findByStage(stageId));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<byte[]> genererPdf(@PathVariable Long id) {
        ConventionResponse convention = conventionService.findById(id);
        byte[] pdf = conventionPdfService.genererPdf(convention);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"convention-" + convention.getNumero() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}