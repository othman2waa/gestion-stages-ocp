package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.Repository.CandidatureRepository;
import com.OCP.Gestion_Stages.Repository.DepartementRepository;
import com.OCP.Gestion_Stages.Repository.EncadrantRepository;
import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.Repository.StagiaireRepository;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.model.Candidature;
import com.OCP.Gestion_Stages.domain.model.Encadrant;
import com.OCP.Gestion_Stages.domain.model.Stagiaire;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chatbot de l'espace encadrant : à partir d'un besoin exprimé en langage naturel,
 * classe les candidatures du département par adéquation, selon une architecture
 * <b>retrieve-and-rerank</b> en deux étages :
 * <ol>
 *   <li><b>Retrieval sémantique</b> — tout le vivier est vectorisé (embeddings) et trié par
 *       similarité cosinus avec le besoin. Rapide, parallélisé et mis en cache.</li>
 *   <li><b>Reranking LLM</b> — seule la short-list des plus pertinents est jugée finement par le
 *       LLM ({@code score} + justification), ce qui borne les appels coûteux.</li>
 * </ol>
 * Le score final est <b>hybride et explicable</b> : {@code W_LLM × score LLM + W_SEM × similarité}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final EncadrantRepository encadrantRepository;
    private final CandidatureRepository candidatureRepository;
    private final StageRepository stageRepository;
    private final StagiaireRepository stagiaireRepository;
    private final DepartementRepository departementRepository;
    private final OllamaService ollamaService;
    private final FileStorageService fileStorageService;
    private final com.OCP.Gestion_Stages.Service.RemunerationService remunerationService;

    private static final int SHORTLIST = 6;    // nb de candidats envoyés au LLM pour le rerank précis
    private static final double W_LLM = 0.65;  // poids du jugement LLM dans le score final
    private static final double W_SEM = 0.35;  // poids de la similarité sémantique (embeddings)

    /** Cache d'embeddings de CV (clé = chemin/identité du CV) : un même CV n'est vectorisé qu'une fois. */
    private final Map<String, float[]> cvEmbeddingCache = new ConcurrentHashMap<>();

    /** Candidat enrichi de son profil textuel et de son score de similarité sémantique (étage 1). */
    private record CandidatScore(Candidature candidature, String profil, double similarite) {}

    // ════════════════════════════════════════════════════════════
    //  Assistant RH : réponses et mini-rapports à partir des indicateurs
    // ════════════════════════════════════════════════════════════
    public Map<String, Object> assistantRh(String sessionKey, String question) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> stats = collecterStats();
        result.put("stats", stats);
        if (question == null || question.isBlank()) {
            result.put("message", "Posez-moi une question sur les candidatures, les stages, la conformité… ou demandez un résumé de la situation.");
            return result;
        }
        // Contexte = indicateurs chiffrés + liste nominative des fiches OCP non remplies.
        // Mémoire conversationnelle : ce contexte n'est réellement envoyé qu'au 1er tour de la session.
        String contexte = formatContexte(stats) + formatFiches() + formatRemuneration();
        result.put("message", ollamaService.repondreAvecMemoire(sessionKey, contexte, question));
        return result;
    }

    /** Démarre un nouveau fil de conversation (oublie l'historique et rafraîchit les données). */
    public void reinitialiserConversation(String sessionKey) {
        ollamaService.reinitialiserConversation(sessionKey);
    }

    private Map<String, Object> collecterStats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("candidatures_total", candidatureRepository.count());
        s.put("candidatures_en_attente", candidatureRepository.countByStatut("EN_ATTENTE"));
        s.put("candidatures_meeting_planifie", candidatureRepository.countByStatut("MEETING_PLANIFIE"));
        s.put("candidatures_acceptees_encadrant", candidatureRepository.countByStatut("ACCEPTEE_ENCADRANT"));
        s.put("candidatures_documents_soumis", candidatureRepository.countByStatut("DOCUMENTS_SOUMIS"));
        s.put("candidatures_validees_rh", candidatureRepository.countByStatut("ACCEPTEE_RH"));
        s.put("candidatures_refusees", candidatureRepository.countByStatut("REFUSEE_RH")
                + candidatureRepository.countByStatut("REFUSEE_ENCADRANT"));
        s.put("stages_total", stageRepository.count());
        s.put("stages_valides", stageRepository.countByStatut(StageStatus.VALIDEE));
        s.put("stages_convention_generee", stageRepository.countByStatut(StageStatus.CONVENTION_GENEREE));
        s.put("stages_convention_signee", stageRepository.countByStatut(StageStatus.CONVENTION_SIGNEE));
        s.put("stages_en_cours", stageRepository.countByStatut(StageStatus.EN_COURS));
        s.put("stages_termines", stageRepository.countByStatut(StageStatus.TERMINE));
        s.put("stagiaires_total", stagiaireRepository.count());
        s.put("encadrants_total", encadrantRepository.count());
        s.put("departements_total", departementRepository.count());
        // Suivi de la Fiche de renseignement OCP (parmi les stagiaires disposant d'un compte)
        List<Stagiaire> avecCompte = stagiairesAvecCompte();
        long ficheOk = avecCompte.stream()
                .filter(st -> Boolean.TRUE.equals(st.getFicheRenseignementCompletee())).count();
        s.put("stagiaires_avec_compte", (long) avecCompte.size());
        s.put("fiches_ocp_completees", ficheOk);
        s.put("fiches_ocp_non_remplies", (long) avecCompte.size() - ficheOk);
        return s;
    }

    private String formatContexte(Map<String, Object> stats) {
        StringBuilder sb = new StringBuilder();
        stats.forEach((k, v) -> sb.append("- ").append(k.replace('_', ' ')).append(" : ").append(v).append("\n"));
        return sb.toString();
    }

    /** Stagiaires possédant un compte (donc censés remplir la fiche). */
    private List<Stagiaire> stagiairesAvecCompte() {
        return stagiaireRepository.findAll().stream()
                .filter(st -> st.getUser() != null).toList();
    }

    /** Listes nominatives (ont rempli / n'ont pas rempli) de la fiche OCP, injectées dans le contexte. */
    private String formatFiches() {
        List<Stagiaire> avecCompte = stagiairesAvecCompte();
        List<String> avecFiche = avecCompte.stream()
                .filter(st -> Boolean.TRUE.equals(st.getFicheRenseignementCompletee()))
                .map(st -> (safe(st.getPrenom()) + " " + safe(st.getNom())).trim())
                .filter(n -> !n.isBlank()).sorted().toList();
        List<String> sansFiche = avecCompte.stream()
                .filter(st -> !Boolean.TRUE.equals(st.getFicheRenseignementCompletee()))
                .map(st -> (safe(st.getPrenom()) + " " + safe(st.getNom())).trim())
                .filter(n -> !n.isBlank()).sorted().toList();

        StringBuilder sb = new StringBuilder();
        sb.append("\nFiche de renseignement OCP (").append(avecCompte.size())
          .append(" stagiaires avec compte) :\n");
        sb.append("- Stagiaires AYANT rempli la fiche (").append(avecFiche.size()).append(") :\n");
        if (avecFiche.isEmpty()) sb.append("    - aucun\n");
        else for (String n : avecFiche) sb.append("    - ").append(n).append("\n");
        sb.append("- Stagiaires N'AYANT PAS rempli la fiche (").append(sansFiche.size()).append(") :\n");
        if (sansFiche.isEmpty()) sb.append("    - aucun (tous l'ont remplie)\n");
        else for (String n : sansFiche) sb.append("    - ").append(n).append("\n");
        return sb.toString();
    }

    /** Données de rémunération PFE (tranche courante), injectées dans le contexte de l'assistant. */
    @SuppressWarnings("unchecked")
    private String formatRemuneration() {
        try {
            Map<String, Object> r = remunerationService.genererListe(null, null, null);
            StringBuilder sb = new StringBuilder("\nRémunération des stages PFE (OCP rémunère les PFE Bac+5) — ");
            sb.append(r.getOrDefault("libelle", "tranche courante")).append(" :\n");
            sb.append("- nombre de stagiaires PFE Bac+5 à rémunérer : ").append(r.getOrDefault("total", 0)).append("\n");
            Object pd = r.get("parDepartement");
            if (pd instanceof List<?> groupes && !groupes.isEmpty()) {
                sb.append("- répartition par département :\n");
                for (Object g : groupes) {
                    Map<String, Object> gm = (Map<String, Object>) g;
                    sb.append("    - ").append(gm.get("departement")).append(" : ")
                      .append(gm.get("total")).append("\n");
                }
            }
            sb.append("(La rémunération est versée à la fin du stage, par tranches semestrielles : "
                    + "juillet et septembre.)\n");
            return sb.toString();
        } catch (Exception e) {
            log.warn("Contexte rémunération indisponible : {}", e.getMessage());
            return "";
        }
    }

    public Map<String, Object> matchCandidats(String username, String besoin) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("besoin", besoin);
        result.put("candidats", List.of());

        if (besoin == null || besoin.isBlank()) {
            result.put("message", "Décrivez votre besoin (profil, compétences, niveau, disponibilité…).");
            return result;
        }

        Encadrant enc = encadrantRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Encadrant introuvable"));
        if (enc.getDepartement() == null) {
            result.put("message", "Aucun département n'est rattaché à votre profil.");
            return result;
        }

        List<Candidature> cands = candidatureRepository.findByDepartementIdAndStatutInOrderByScoreMatchingDesc(
                enc.getDepartement().getId(), List.of("EN_ATTENTE", "MEETING_PLANIFIE"));
        if (cands.isEmpty()) {
            result.put("message", "Aucune candidature en attente dans votre département.");
            return result;
        }

        // ── Étage 1 : RETRIEVAL sémantique sur TOUT le vivier (embeddings, rapide & parallèle) ──
        final float[] besoinVec = ollamaService.embed(besoin);
        List<CandidatScore> classement;
        if (besoinVec != null) {
            classement = cands.parallelStream()
                    .map(c -> {
                        String profil = profilTexte(c);
                        double sim = ollamaService.cosineSimilarity(besoinVec, embeddingCV(c, profil)); // 0..1
                        return new CandidatScore(c, profil, sim);
                    })
                    .sorted((a, b) -> Double.compare(b.similarite(), a.similarite()))
                    .toList();
        } else {
            // Dégradation gracieuse : embeddings indisponibles → on conserve le pré-tri SQL (score de dépôt)
            classement = cands.stream()
                    .map(c -> new CandidatScore(c, profilTexte(c), -1))
                    .toList();
        }
        int vivierAnalyse = classement.size();

        // ── Étage 2 : RERANK précis par le LLM, uniquement sur la short-list la plus pertinente ──
        List<Map<String, Object>> matches = new ArrayList<>();
        for (CandidatScore cs : classement.stream().limit(SHORTLIST).toList()) {
            Candidature c = cs.candidature();
            Map<String, Object> ia = ollamaService.matchBesoin(cs.profil(), besoin);
            int scoreLlm = ((Number) ia.getOrDefault("score", 0)).intValue();
            int scoreSem = cs.similarite() >= 0 ? (int) Math.round(cs.similarite() * 100) : scoreLlm;
            int scoreFinal = cs.similarite() >= 0
                    ? (int) Math.round(W_LLM * scoreLlm + W_SEM * scoreSem)
                    : scoreLlm;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("candidatureId", c.getId());
            m.put("nom", c.getNom());
            m.put("prenom", c.getPrenom());
            m.put("filiere", c.getFiliere());
            m.put("niveau", c.getNiveau());
            m.put("score", scoreFinal);          // score hybride explicable
            m.put("scoreLLM", scoreLlm);          // jugement du LLM (0-100)
            m.put("scoreSemantique", scoreSem);   // similarité embeddings (0-100)
            m.put("justification", ia.get("justification"));
            matches.add(m);
        }
        matches.sort((a, b) -> ((Number) b.get("score")).intValue() - ((Number) a.get("score")).intValue());

        result.put("count", matches.size());
        result.put("vivierAnalyse", vivierAnalyse);
        result.put("methode", besoinVec != null
                ? "retrieval sémantique (embeddings) + rerank LLM"
                : "rerank LLM (embeddings indisponibles)");
        result.put("candidats", matches);
        result.put("message", besoinVec != null
                ? "J'ai comparé sémantiquement " + vivierAnalyse + " candidature(s) de votre département, "
                  + "puis analysé en détail les " + matches.size()
                  + " plus pertinentes — classées par adéquation à votre besoin."
                : "J'ai analysé " + matches.size()
                  + " candidature(s) de votre département, classées par adéquation à votre besoin.");
        return result;
    }

    /**
     * Profil textuel d'un candidat : texte du CV <b>pré-calculé à l'ingestion</b> si disponible,
     * sinon extraction live (PDFBox), sinon ses champs structurés.
     */
    private String profilTexte(Candidature c) {
        String txt = (c.getCvTexte() != null && !c.getCvTexte().isBlank()) ? c.getCvTexte() : extraireCv(c);
        return !txt.isBlank() ? txt
                : String.join(" ", safe(c.getFiliere()), safe(c.getNiveau()),
                        safe(c.getSpecialite()), safe(c.getSujetSouhaite()));
    }

    /**
     * Embedding du CV. Cas nominal : on lit le vecteur <b>pré-calculé et persisté</b> à l'ingestion
     * (lecture instantanée, aucun appel Ollama). Repli pour une candidature antérieure non indexée :
     * calcul live, mémorisé dans un cache de session.
     */
    private float[] embeddingCV(Candidature c, String profil) {
        float[] stored = ollamaService.embeddingFromJson(c.getCvEmbedding());
        if (stored != null) return stored;
        String cle = c.getCvChemin() != null ? c.getCvChemin() : ("cand-" + c.getId());
        float[] cached = cvEmbeddingCache.get(cle);
        if (cached != null) return cached;
        float[] v = ollamaService.embed(profil);
        if (v != null) cvEmbeddingCache.put(cle, v);
        return v;
    }

    private String extraireCv(Candidature c) {
        try {
            byte[] bytes = c.getCvChemin() != null ? fileStorageService.read(c.getCvChemin()) : c.getCvContenu();
            if (bytes == null || bytes.length == 0) return "";
            try (org.apache.pdfbox.pdmodel.PDDocument pdf = org.apache.pdfbox.Loader.loadPDF(bytes)) {
                String t = new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);
                return t.length() > 1500 ? t.substring(0, 1500) : t;
            }
        } catch (Exception e) {
            return "";
        }
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}
