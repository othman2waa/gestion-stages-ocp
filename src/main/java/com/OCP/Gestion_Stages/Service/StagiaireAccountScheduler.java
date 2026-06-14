package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.Repository.CompteStagiaireLogRepository;
import com.OCP.Gestion_Stages.Repository.StagiaireRepository;
import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.Repository.UserRepository;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.model.CompteStagiaireLog;
import com.OCP.Gestion_Stages.domain.model.Stage;
import com.OCP.Gestion_Stages.domain.model.Stagiaire;
import com.OCP.Gestion_Stages.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduler pour la gestion du cycle de vie des comptes stagiaires.
 *
 * 1. Quand un stage passe à TERMINE → calcule la date de désactivation (dateFin + 30 jours)
 * 2. Chaque jour à 9h → désactive les comptes dont la date est dépassée
 * 3. Archive automatiquement le stage si pas déjà fait
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StagiaireAccountScheduler {

    private static final int JOURS_GRACE = 30;

    private final StageRepository stageRepository;
    private final StagiaireRepository stagiaireRepository;
    private final UserRepository userRepository;
    private final CompteStagiaireLogRepository logRepository;
    private final ArchiveStageService archiveService;

    /**
     * Étape 1 : Programmer la désactivation pour les stages TERMINE
     * sans date de désactivation calculée.
     * Tourne chaque jour à 8h30.
     */
    @Scheduled(cron = "0 30 8 * * *")
    @Transactional
    public void programmerDesactivations() {
        List<Stage> stagesTermines = stageRepository.findByStatut(StageStatus.TERMINE);

        int count = 0;
        for (Stage stage : stagesTermines) {
            Stagiaire stagiaire = stage.getStagiaire();
            if (stagiaire == null || stagiaire.getDateDesactivationPrevue() != null) continue;

            // Date de désactivation = dateFin du stage + 30 jours (ou aujourd'hui + 30 si dateFin est null)
            LocalDate dateFin = stage.getDateFin() != null ? stage.getDateFin() : LocalDate.now();
            LocalDate dateDesactivation = dateFin.plusDays(JOURS_GRACE);

            // Si la date est déjà passée (stage ancien), on met aujourd'hui + 7 jours de grâce
            if (dateDesactivation.isBefore(LocalDate.now())) {
                dateDesactivation = LocalDate.now().plusDays(7);
            }

            stagiaire.setDateDesactivationPrevue(dateDesactivation);
            stagiaireRepository.save(stagiaire);
            count++;
        }

        if (count > 0) {
            log.info("Désactivation programmée pour {} stagiaire(s)", count);
        }
    }

    /**
     * Étape 2 : Désactiver les comptes dont la date est dépassée.
     * Tourne chaque jour à 9h00.
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void desactiverComptes() {
        // Trouver les stages TERMINE dont le stagiaire a un compte actif et une date dépassée
        List<Stage> stagesTermines = stageRepository.findByStatut(StageStatus.TERMINE);

        int count = 0;
        for (Stage stage : stagesTermines) {
            Stagiaire stagiaire = stage.getStagiaire();
            if (stagiaire == null) continue;

            User user = stagiaire.getUser();
            if (user == null || !user.getActif()) continue;

            LocalDate dateDesactivation = stagiaire.getDateDesactivationPrevue();
            if (dateDesactivation == null || dateDesactivation.isAfter(LocalDate.now())) continue;

            // Désactiver le compte
            user.setActif(false);
            userRepository.save(user);

            // Archiver le stage si pas encore fait
            try {
                archiveService.archiverStage(stage, "SYSTEME_AUTO");
            } catch (Exception e) {
                log.warn("Archivage échoué pour stage {} : {}", stage.getId(), e.getMessage());
            }

            // Logger la désactivation
            CompteStagiaireLog logEntry = CompteStagiaireLog.builder()
                    .stagiaire(stagiaire)
                    .user(user)
                    .stage(stage)
                    .action("DESACTIVATION_AUTO")
                    .dateFinStage(stage.getDateFin())
                    .details("Désactivation automatique " + JOURS_GRACE + " jours après fin de stage. Stage: " + stage.getSujet())
                    .build();
            logRepository.save(logEntry);

            count++;
            log.info("Compte désactivé : {} {} (stage: {}, fin: {})",
                    stagiaire.getPrenom(), stagiaire.getNom(),
                    stage.getSujet(), stage.getDateFin());
        }

        if (count > 0) {
            log.info("Total comptes désactivés aujourd'hui : {}", count);
        }
    }
}
