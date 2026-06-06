package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.Repository.CandidatureRepository;
import com.OCP.Gestion_Stages.Repository.DocumentCandidatureRepository;
import com.OCP.Gestion_Stages.Repository.DocumentStagiaireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * Job ONE-SHOT : déplace les fichiers encore stockés en base (bytea) vers le disque.
 *
 * Désactivé par défaut. Pour le lancer une fois :
 *   - via env  : APP_STORAGE_MIGRATE_ON_STARTUP=true
 *   - ou args  : --app.storage.migrate-on-startup=true
 *
 * Idempotent : ne traite que les lignes ayant un contenu en base sans chemin disque.
 * Chaque ligne est migrée dans sa propre transaction (voir StorageMigrationService) :
 * le fichier est écrit d'abord, puis la ligne mise à jour ; en cas d'erreur, on log,
 * la ligne est préservée (bytea intact) et on continue.
 *
 * Après exécution : VACUUM FULL côté PostgreSQL pour récupérer l'espace disque.
 */
@Component
@ConditionalOnProperty(name = "app.storage.migrate-on-startup", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class StorageMigrationRunner implements CommandLineRunner {

    private final DocumentStagiaireRepository documentStagiaireRepository;
    private final DocumentCandidatureRepository documentCandidatureRepository;
    private final CandidatureRepository candidatureRepository;
    private final StorageMigrationService migrationService;

    @Override
    public void run(String... args) {
        log.warn("════════ MIGRATION bytea → disque : DÉBUT ════════");
        int total = 0;
        total += migrer("DocumentStagiaire",
                documentStagiaireRepository.findIdsAMigrer(), migrationService::migrerDocStagiaire);
        total += migrer("DocumentCandidature",
                documentCandidatureRepository.findIdsAMigrer(), migrationService::migrerDocCandidature);
        total += migrer("CV candidature",
                candidatureRepository.findCvIdsAMigrer(), migrationService::migrerCvCandidature);
        log.warn("════════ MIGRATION TERMINÉE : {} fichiers déplacés vers le disque ════════", total);
        log.warn("Pense à lancer 'VACUUM FULL' sur PostgreSQL pour libérer l'espace.");
    }

    private int migrer(String libelle, List<Long> ids, Function<Long, Boolean> migrateOne) {
        int ok = 0, ko = 0;
        for (Long id : ids) {
            try {
                if (migrateOne.apply(id)) ok++;
            } catch (Exception e) {
                ko++;
                log.error("{} {} : migration échouée ({}) — bytea conservé", libelle, id, e.getMessage());
            }
        }
        log.warn("{} : {} migrés, {} échecs (sur {} candidats)", libelle, ok, ko, ids.size());
        return ok;
    }
}
