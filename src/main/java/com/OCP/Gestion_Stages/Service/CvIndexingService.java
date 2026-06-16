package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.Repository.CandidatureRepository;
import com.OCP.Gestion_Stages.domain.model.Candidature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Indexation IA du CV d'une candidature, exécutée EN TÂCHE DE FOND dès le dépôt :
 * extraction du texte (PDFBox) + calcul de l'embedding vectoriel (Ollama), persistés sur la
 * candidature. Le travail lourd n'est ainsi fait <b>qu'une seule fois</b> à l'ingestion ; la
 * recherche sémantique de l'encadrant se contente ensuite de lire ces données pré-calculées,
 * d'où des résultats quasi instantanés.
 *
 * <p>Méthode {@code @Async} placée dans un bean distinct du service appelant : le proxy Spring
 * n'intercepte l'asynchronisme que sur un appel inter-beans (pas en auto-invocation).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CvIndexingService {

    private final CandidatureRepository candidatureRepository;
    private final OllamaService ollamaService;
    private final FileStorageService fileStorageService;

    /** Événement publié à la création d'une candidature ; consommé une fois la transaction validée. */
    public record CandidatureCreatedEvent(Long candidatureId) {}

    /**
     * Indexe le CV <b>après le commit</b> de la transaction de création : le
     * {@code @TransactionalEventListener(AFTER_COMMIT)} garantit que la candidature est bien
     * persistée et visible, et le {@code @Async} exécute le traitement hors du thread appelant.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCandidatureCreated(CandidatureCreatedEvent event) {
        indexerAsync(event.candidatureId());
    }

    /** Indexation directe (réutilisable pour un re-traitement manuel / backfill). */
    @Async
    public void indexerAsync(Long candidatureId) {
        candidatureRepository.findById(candidatureId).ifPresent(this::indexer);
    }

    /** Nombre de candidatures possédant un CV mais pas encore indexées (pour l'aperçu avant backfill). */
    public long compterNonIndexees() {
        return candidatureRepository.findAll().stream().filter(this::aIndexer).count();
    }

    /**
     * Réindexe en arrière-plan toutes les candidatures non encore traitées (ayant un CV).
     * Séquentiel dans un seul thread async pour ne pas saturer Ollama d'appels concurrents.
     */
    @Async
    public void backfillAsync() {
        var aTraiter = candidatureRepository.findAll().stream().filter(this::aIndexer).toList();
        log.info("Backfill CV : {} candidature(s) à indexer.", aTraiter.size());
        int ok = 0;
        for (Candidature c : aTraiter) {
            indexer(c);
            ok++;
        }
        log.info("Backfill CV terminé : {} candidature(s) traitée(s).", ok);
    }

    /** A un CV exploitable et n'a pas encore été indexée. */
    private boolean aIndexer(Candidature c) {
        boolean aCv = c.getCvChemin() != null || c.getCvContenu() != null;
        return aCv && !Boolean.TRUE.equals(c.getCvTraite());
    }

    /** Extrait le texte du CV, calcule son embedding, et persiste le tout (idempotent). */
    public void indexer(Candidature c) {
        try {
            String texte = extraireTexteCv(c);
            if (texte != null && !texte.isBlank()) {
                // On borne le texte stocké (en-tête, formation, compétences suffisent au matching).
                c.setCvTexte(texte.length() > 4000 ? texte.substring(0, 4000) : texte);
                float[] vecteur = ollamaService.embed(texte);
                if (vecteur != null) c.setCvEmbedding(ollamaService.embeddingToJson(vecteur));
            }
            c.setCvTraite(true);
            candidatureRepository.save(c);
            log.info("CV indexé (candidature {}) : texte={}, embedding={}",
                    c.getId(), c.getCvTexte() != null, c.getCvEmbedding() != null);
        } catch (Exception e) {
            log.warn("Indexation CV échouée (candidature {}) : {}", c.getId(), e.getMessage());
        }
    }

    /** Lecture des octets du CV (disque ou bytea) + extraction PDFBox. */
    private String extraireTexteCv(Candidature c) {
        try {
            byte[] bytes = c.getCvChemin() != null ? fileStorageService.read(c.getCvChemin()) : c.getCvContenu();
            if (bytes == null || bytes.length == 0) return "";
            try (org.apache.pdfbox.pdmodel.PDDocument pdf = org.apache.pdfbox.Loader.loadPDF(bytes)) {
                return new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);
            }
        } catch (Exception e) {
            log.warn("Extraction texte CV échouée (candidature {}) : {}", c.getId(), e.getMessage());
            return "";
        }
    }
}
