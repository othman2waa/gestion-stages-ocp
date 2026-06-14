package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.enums.TypeStage;
import com.OCP.Gestion_Stages.domain.model.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reporting")
@RequiredArgsConstructor
public class ReportingController {

    private final StageRepository stageRepository;
    private final StagiaireRepository stagiaireRepository;
    private final ConventionRepository conventionRepository;
    private final EvaluationRepository evaluationRepository;
    private final CandidatureRepository candidatureRepository;
    private final AnnonceStageRepository annonceRepository;
    private final SuiviHebdomadaireRepository suiviRepository;
    private final EncadrantRepository encadrantRepository;


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




    @GetMapping("/stats-completes")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<Map<String, Object>> getStatsCompletes() {
        Map<String, Object> stats = new HashMap<>();

        // KPIs généraux — COUNT directs
        stats.put("totalStagiaires",   stagiaireRepository.count());
        stats.put("totalStages",       stageRepository.count());
        stats.put("totalConventions",  conventionRepository.count());
        stats.put("totalEvaluations",  evaluationRepository.count());
        stats.put("totalCandidatures", candidatureRepository.count());
        stats.put("totalAnnonces",     annonceRepository.count());
        stats.put("totalSuivis",       suiviRepository.count());

        // Encadrants
        stats.put("totalEncadrants", encadrantRepository.count());

        // Stages par statut — COUNT directs
        stats.put("stagesEnAttente",          stageRepository.countByStatut(StageStatus.EN_ATTENTE));
        stats.put("stagesEnCours",            stageRepository.countByStatut(StageStatus.EN_COURS));
        stats.put("stagesTermines",           stageRepository.countByStatut(StageStatus.TERMINE));
        stats.put("stagesValides",            stageRepository.countByStatut(StageStatus.VALIDEE));
        stats.put("stagesAnnules",            stageRepository.countByStatut(StageStatus.ANNULE));
        stats.put("stagesConventionGeneree",  stageRepository.countByStatut(StageStatus.CONVENTION_GENEREE));
        stats.put("stagesConventionSignee",   stageRepository.countByStatut(StageStatus.CONVENTION_SIGNEE));
        stats.put("stagesEnAttenteEvaluation",stageRepository.countByStatut(StageStatus.EN_ATTENTE_EVALUATION));

        // Candidatures — COUNT directs (1 seule requête chacun)
        long candEnAttente = candidatureRepository.countByStatut("EN_ATTENTE");
        long candAcceptees = candidatureRepository.countByStatut("ACCEPTEE");
        long candRefusees  = candidatureRepository.countByStatut("REFUSEE");
        stats.put("candidaturesEnAttente", candEnAttente);
        stats.put("candidaturesAcceptees", candAcceptees);
        stats.put("candidaturesRefusees",  candRefusees);

        // Taux d'acceptation
        long totalCand = candidatureRepository.count();
        stats.put("tauxAcceptation", totalCand > 0 ? (candAcceptees * 100 / totalCand) : 0);

        // Moyenne évaluations
        stats.put("moyenneEvaluations", evaluationRepository.findAverageNote());

        // Score moyen matching — requête directe
        stats.put("scoreMoyenMatching", Math.round(candidatureRepository.findAverageScoreMatching()));

        // Par département et type
        stats.put("stagesParDepartement", stageRepository.countByDepartement());
        stats.put("stagesParType",        stageRepository.countByTypeStage());

        // Stagiaires par école / établissement d'origine
        stats.put("stagiairesParEcole",   stagiaireRepository.countByEtablissement());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/pfe-bac5")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<Map<String, Object>>> getRapportPfeBac5(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Long departementId) {

        List<Stage> stages = stageRepository.findByTypeStageWithDetails(TypeStage.PFE);

        List<Map<String, Object>> result = stages.stream()
                .filter(s -> s.getStagiaire() != null)
                .filter(s -> annee == null || (s.getDateDebut() != null && s.getDateDebut().getYear() == annee))
                .filter(s -> departementId == null || (s.getDepartement() != null && s.getDepartement().getId().equals(departementId)))
                .map(s -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", s.getId());
                    row.put("nom", s.getStagiaire().getNom());
                    row.put("prenom", s.getStagiaire().getPrenom());
                    row.put("etablissement", s.getStagiaire().getEtablissement() != null
                            ? s.getStagiaire().getEtablissement().getNom() : "—");
                    row.put("departement", s.getDepartement() != null ? s.getDepartement().getNom() : "—");
                    row.put("sujet", s.getSujet());
                    row.put("dateDebut", s.getDateDebut());
                    row.put("dateFin", s.getDateFin());
                    row.put("dureeMois", s.getDateDebut() != null && s.getDateFin() != null
                            ? ChronoUnit.MONTHS.between(s.getDateDebut(), s.getDateFin()) : null);
                    row.put("encadrant", s.getEncadrant() != null
                            ? s.getEncadrant().getPrenom() + " " + s.getEncadrant().getNom() : "—");
                    row.put("statut", s.getStatut().name());
                    row.put("niveau", s.getStagiaire().getNiveau());
                    return row;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}