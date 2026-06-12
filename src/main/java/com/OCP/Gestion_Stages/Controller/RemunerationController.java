package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Service.RemunerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Extraction périodique des stagiaires éligibles à la rémunération OCP
 * (PFE Bac+5, terminé dans les N derniers mois, attestation validée).
 */
@RestController
@RequestMapping("/api/remuneration")
@RequiredArgsConstructor
public class RemunerationController {

    private final RemunerationService remunerationService;

    @GetMapping("/eligibles")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<Map<String, Object>> eligibles(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) String tranche,
            @RequestParam(required = false) Integer mois) {
        return ResponseEntity.ok(remunerationService.genererListe(annee, tranche, mois));
    }
}
