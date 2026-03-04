package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.Repository.StagiaireRepository;
import com.OCP.Gestion_Stages.Repository.ConventionRepository;
import com.OCP.Gestion_Stages.Repository.EvaluationRepository;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reporting")
@RequiredArgsConstructor
public class ReportingController {

    private final StageRepository stageRepository;
    private final StagiaireRepository stagiaireRepository;
    private final ConventionRepository conventionRepository;
    private final EvaluationRepository evaluationRepository;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalStagiaires", stagiaireRepository.count());
        stats.put("totalStages", stageRepository.count());
        stats.put("totalConventions", conventionRepository.count());
        stats.put("totalEvaluations", evaluationRepository.count());

        stats.put("stagesEnAttente",
                stageRepository.countByStatut(StageStatus.EN_ATTENTE));
        stats.put("stagesEnCours",
                stageRepository.countByStatut(StageStatus.EN_COURS));
        stats.put("stagesTermines",
                stageRepository.countByStatut(StageStatus.TERMINE));

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stages-par-departement")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<Map<String, Object>> getStagesParDepartement() {
        Map<String, Object> result = new HashMap<>();
        result.put("data", stageRepository.countByDepartement());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stages-par-type")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<Map<String, Object>> getStagesParType() {
        Map<String, Object> result = new HashMap<>();
        result.put("data", stageRepository.countByTypeStage());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/evaluations-moyennes")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<Map<String, Object>> getEvaluationsMoyennes() {
        Map<String, Object> result = new HashMap<>();
        result.put("moyenneGenerale", evaluationRepository.findAverageNote());
        return ResponseEntity.ok(result);
    }
}