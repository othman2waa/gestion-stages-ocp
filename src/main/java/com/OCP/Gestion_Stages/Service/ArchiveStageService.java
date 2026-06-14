package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.Repository.ArchiveStageRepository;
import com.OCP.Gestion_Stages.Repository.EvaluationRepository;
import com.OCP.Gestion_Stages.Repository.DocumentStagiaireRepository;
import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.Repository.AuditLogRepository;
import com.OCP.Gestion_Stages.domain.model.ArchiveStage;
import com.OCP.Gestion_Stages.domain.model.AuditLog;
import com.OCP.Gestion_Stages.domain.model.DocumentStagiaire;
import com.OCP.Gestion_Stages.domain.model.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveStageService {

    private final ArchiveStageRepository archiveRepository;
    private final EvaluationRepository evaluationRepository;
    private final DocumentStagiaireRepository documentStagiaireRepository;
    private final StageRepository stageRepository;
    private final AuditLogRepository auditLogRepository;
    private final FileStorageService fileStorageService;

    @Value("${app.retention.years:1}")
    private int retentionYears;

    @Transactional
    public ArchiveStage archiverStage(Stage stage, String archivePar) {
        // Éviter les doublons
        if (archiveRepository.existsByStageId(stage.getId())) {
            return archiveRepository.findByStageId(stage.getId()).orElseThrow();
        }

        // Calculer note finale depuis évaluations
        Double noteFinale = evaluationRepository.findAverageNoteByStageId(stage.getId());
        String mention = calculerMention(noteFinale);

        ArchiveStage archive = ArchiveStage.builder()
                .stageId(stage.getId())
                // Stagiaire
                .stagiaireNom(stage.getStagiaire() != null ? stage.getStagiaire().getNom() : "")
                .stagiairePrenom(stage.getStagiaire() != null ? stage.getStagiaire().getPrenom() : "")
                .stagiaireEmail(stage.getStagiaire() != null ? stage.getStagiaire().getEmail() : "")
                .stagiaireFiliere(stage.getStagiaire() != null ? stage.getStagiaire().getFiliere() : "")
                .stagiaireNiveau(stage.getStagiaire() != null ? stage.getStagiaire().getNiveau() : "")
                .stagiaireEtablissement(stage.getStagiaire() != null && stage.getStagiaire().getEtablissement() != null
                        ? stage.getStagiaire().getEtablissement().getNom() : "")
                // Encadrant
                .encadrantNom(stage.getEncadrant() != null ? stage.getEncadrant().getNom() : "")
                .encadrantPrenom(stage.getEncadrant() != null ? stage.getEncadrant().getPrenom() : "")
                .encadrantEmail(stage.getEncadrant() != null ? stage.getEncadrant().getEmail() : "")
                // Stage
                .departementNom(stage.getDepartement() != null ? stage.getDepartement().getNom() : "")
                .sujet(stage.getSujet())
                .typeStage(stage.getTypeStage() != null ? stage.getTypeStage().name() : "")
                .dateDebut(stage.getDateDebut())
                .dateFin(stage.getDateFin())
                // Résultats
                .noteFinale(noteFinale != null ? BigDecimal.valueOf(noteFinale) : null)
                .mention(mention)
                // Archive
                .anneeStage(stage.getDateFin() != null ? stage.getDateFin().getYear()
                        : LocalDateTime.now().getYear())
                .dateArchivage(LocalDateTime.now())
                .archivePar(archivePar)
                .build();

        log.info("Stage {} archivé par {}", stage.getId(), archivePar);
        return archiveRepository.save(archive);
    }

    /**
     * Purge de rétention : pour chaque archive dont le stage s'est terminé il y a plus de
     * {@code app.retention.years}, on supprime les documents du stagiaire (fichiers + lignes)
     * et on anonymise l'archive (effacement de l'identité), en conservant les champs KPI.
     * Renvoie le nombre d'archives anonymisées.
     */
    @Transactional
    public int purgerArchivesExpirees(String declenchePar) {
        LocalDate seuil = LocalDate.now().minusYears(retentionYears);
        List<ArchiveStage> expirees = archiveRepository.findAllByOrderByDateArchivageDesc().stream()
                .filter(a -> !Boolean.TRUE.equals(a.getAnonymise()))
                .filter(a -> a.getDateFin() != null && a.getDateFin().isBefore(seuil))
                .collect(Collectors.toList());

        int docsSupprimes = 0;
        for (ArchiveStage a : expirees) {
            docsSupprimes += supprimerDocumentsDuStage(a.getStageId());
            // Anonymisation : on efface les données personnelles, on garde les champs KPI
            a.setStagiaireNom("[anonymisé]");
            a.setStagiairePrenom("");
            a.setStagiaireEmail(null);
            a.setStagiaireEtablissement(null);
            a.setEncadrantNom("[anonymisé]");
            a.setEncadrantPrenom("");
            a.setEncadrantEmail(null);
            a.setSujet(null);
            a.setAnonymise(true);
            a.setDateAnonymisation(LocalDateTime.now());
            archiveRepository.save(a);
        }

        if (!expirees.isEmpty()) {
            try {
                auditLogRepository.save(AuditLog.builder()
                        .action("PURGE_ARCHIVE")
                        .utilisateur(declenchePar)
                        .details("Anonymisation de " + expirees.size() + " archive(s) > " + retentionYears
                                + " an(s) ; " + docsSupprimes + " document(s) supprimé(s).")
                        .build());
            } catch (Exception e) {
                log.warn("Journal d'audit de la purge non écrit : {}", e.getMessage());
            }
        }
        log.info("Purge rétention : {} archive(s) anonymisée(s), {} document(s) supprimé(s) (par {})",
                expirees.size(), docsSupprimes, declenchePar);
        return expirees.size();
    }

    /** Supprime les documents (fichiers + lignes) du stagiaire rattaché au stage archivé. */
    private int supprimerDocumentsDuStage(Long stageId) {
        if (stageId == null) return 0;
        var stageOpt = stageRepository.findById(stageId);
        if (stageOpt.isEmpty() || stageOpt.get().getStagiaire() == null) return 0;
        Long stagiaireId = stageOpt.get().getStagiaire().getId();
        List<DocumentStagiaire> docs = documentStagiaireRepository.findByStagiaireId(stagiaireId);
        int n = 0;
        for (DocumentStagiaire d : docs) {
            try {
                if (d.getCheminFichier() != null) fileStorageService.delete(d.getCheminFichier());
            } catch (Exception e) {
                log.warn("Fichier non supprimé ({}) : {}", d.getCheminFichier(), e.getMessage());
            }
            documentStagiaireRepository.delete(d);
            n++;
        }
        return n;
    }

    public List<Map<String, Object>> getAll(Integer annee, String departement) {
        List<ArchiveStage> archives;
        if (annee != null && departement != null && !departement.isEmpty()) {
            archives = archiveRepository.findAllByOrderByDateArchivageDesc().stream()
                    .filter(a -> a.getAnneeStage().equals(annee) && departement.equals(a.getDepartementNom()))
                    .collect(Collectors.toList());
        } else if (annee != null) {
            archives = archiveRepository.findByAnneeStageOrderByDateArchivageDesc(annee);
        } else if (departement != null && !departement.isEmpty()) {
            archives = archiveRepository.findByDepartementNomOrderByDateArchivageDesc(departement);
        } else {
            archives = archiveRepository.findAllByOrderByDateArchivageDesc();
        }
        return archives.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        List<ArchiveStage> all = archiveRepository.findAllByOrderByDateArchivageDesc();
        stats.put("total", all.size());
        stats.put("annees", archiveRepository.findDistinctAnnees());
        stats.put("departements", archiveRepository.findDistinctDepartements());
        stats.put("moyenneNote", archiveRepository.findAverageNote());

        // Par type stage
        Map<String, Long> parType = all.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getTypeStage() != null ? a.getTypeStage() : "INCONNU",
                        Collectors.counting()
                ));
        stats.put("parTypeStage", parType);

        // Par département
        Map<String, Long> parDept = all.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getDepartementNom() != null ? a.getDepartementNom() : "Inconnu",
                        Collectors.counting()
                ));
        stats.put("parDepartement", parDept);

        // Par année
        Map<String, Long> parAnnee = all.stream()
                .collect(Collectors.groupingBy(
                        a -> String.valueOf(a.getAnneeStage()),
                        Collectors.counting()
                ));
        stats.put("parAnnee", parAnnee);

        return stats;
    }

    private String calculerMention(Double note) {
        if (note == null) return "Non évalué";
        if (note >= 16) return "Très Bien";
        if (note >= 14) return "Bien";
        if (note >= 12) return "Assez Bien";
        if (note >= 10) return "Passable";
        return "Insuffisant";
    }

    public Map<String, Object> toResponse(ArchiveStage a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("stageId", a.getStageId());
        m.put("stagiaireNom", a.getStagiaireNom() + " " + a.getStagiairePrenom());
        m.put("stagiaireEmail", a.getStagiaireEmail());
        m.put("stagiaireFiliere", a.getStagiaireFiliere());
        m.put("stagiaireNiveau", a.getStagiaireNiveau());
        m.put("stagiaireEtablissement", a.getStagiaireEtablissement());
        m.put("encadrantNom", a.getEncadrantPrenom() + " " + a.getEncadrantNom());
        m.put("departementNom", a.getDepartementNom());
        m.put("sujet", a.getSujet());
        m.put("typeStage", a.getTypeStage());
        m.put("dateDebut", a.getDateDebut() != null ? a.getDateDebut().toString() : "");
        m.put("dateFin", a.getDateFin() != null ? a.getDateFin().toString() : "");
        m.put("noteFinale", a.getNoteFinale());
        m.put("mention", a.getMention());
        m.put("anneeStage", a.getAnneeStage());
        m.put("dateArchivage", a.getDateArchivage() != null ? a.getDateArchivage().toString() : "");
        m.put("archivePar", a.getArchivePar());
        m.put("anonymise", Boolean.TRUE.equals(a.getAnonymise()));
        return m;
    }
}