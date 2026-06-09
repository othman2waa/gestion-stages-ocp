package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.model.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RappelStageScheduler {

    private final StageRepository stageRepository;
    private final EmailService emailService;
    private final ArchiveStageService archiveStageService;

    // Exécute chaque jour à 8h00
    @Scheduled(cron = "0 0 8 * * *")
    public void envoyerRappelsFinStage() {
        LocalDate dans7Jours = LocalDate.now().plusDays(7);
        List<Stage> stages = stageRepository.findByDateFinAndStatut(dans7Jours, StageStatus.EN_COURS);

        log.info("Rappels fin de stage : {} stage(s) trouvé(s)", stages.size());

        for (Stage stage : stages) {
            try {
                if (stage.getStagiaire() != null && stage.getStagiaire().getEmail() != null) {
                    emailService.envoyerRappelFinStage(
                            stage.getStagiaire().getEmail(),
                            stage.getStagiaire().getPrenom() + " " + stage.getStagiaire().getNom(),
                            stage.getSujet(),
                            stage.getDateFin().toString()
                    );
                }
            } catch (Exception e) {
                log.warn("Rappel non envoyé pour stage {} : {}", stage.getId(), e.getMessage());
            }
        }
    }

    // Auto-archivage : 7 jours après la date de fin de stage
    @Scheduled(cron = "0 0 7 * * *")
    public void autoArchiverStages() {
        LocalDate seuilArchivage = LocalDate.now().minusDays(7);
        List<Stage> stages = stageRepository.findByStatutAndDateFinBefore(StageStatus.TERMINE, seuilArchivage);

        log.info("Auto-archivage : {} stage(s) à archiver", stages.size());

        for (Stage stage : stages) {
            try {
                archiveStageService.archiverStage(stage, "SYSTEME_AUTO");
                log.info("Stage {} archivé automatiquement", stage.getId());
            } catch (Exception e) {
                log.warn("Erreur archivage auto stage {} : {}", stage.getId(), e.getMessage());
            }
        }
    }
}