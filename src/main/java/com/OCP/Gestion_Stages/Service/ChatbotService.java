package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.Repository.CandidatureRepository;
import com.OCP.Gestion_Stages.Repository.DepartementRepository;
import com.OCP.Gestion_Stages.Repository.EncadrantRepository;
import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.Repository.StagiaireRepository;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.model.Candidature;
import com.OCP.Gestion_Stages.domain.model.Encadrant;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chatbot de l'espace encadrant : à partir d'un besoin exprimé en langage naturel,
 * classe les candidatures du département de l'encadrant par adéquation (via le LLM).
 * Le nombre de candidatures analysées par le LLM est borné pour rester réactif sur CPU.
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

    private static final int MAX_CANDIDATS = 6; // borne les appels LLM (latence CPU)

    // ════════════════════════════════════════════════════════════
    //  Assistant RH : réponses et mini-rapports à partir des indicateurs
    // ════════════════════════════════════════════════════════════
    public Map<String, Object> assistantRh(String question) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> stats = collecterStats();
        result.put("stats", stats);
        if (question == null || question.isBlank()) {
            result.put("message", "Posez-moi une question sur les candidatures, les stages, la conformité… ou demandez un résumé de la situation.");
            return result;
        }
        result.put("message", ollamaService.repondreAvecContexte(formatContexte(stats), question));
        return result;
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
        return s;
    }

    private String formatContexte(Map<String, Object> stats) {
        StringBuilder sb = new StringBuilder();
        stats.forEach((k, v) -> sb.append("- ").append(k.replace('_', ' ')).append(" : ").append(v).append("\n"));
        return sb.toString();
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

        List<Map<String, Object>> matches = new ArrayList<>();
        for (Candidature c : cands.stream().limit(MAX_CANDIDATS).toList()) {
            String cv = extraireCv(c);
            String profil = !cv.isBlank() ? cv
                    : String.join(" ", safe(c.getFiliere()), safe(c.getNiveau()),
                            safe(c.getSpecialite()), safe(c.getSujetSouhaite()));
            Map<String, Object> ia = ollamaService.matchBesoin(profil, besoin);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("candidatureId", c.getId());
            m.put("nom", c.getNom());
            m.put("prenom", c.getPrenom());
            m.put("filiere", c.getFiliere());
            m.put("niveau", c.getNiveau());
            m.put("score", ia.get("score"));
            m.put("justification", ia.get("justification"));
            matches.add(m);
        }
        matches.sort((a, b) -> ((Number) b.get("score")).intValue() - ((Number) a.get("score")).intValue());

        result.put("count", matches.size());
        result.put("candidats", matches);
        result.put("message", "J'ai analysé " + matches.size()
                + " candidature(s) de votre département. Voici les profils classés par adéquation à votre besoin.");
        return result;
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
