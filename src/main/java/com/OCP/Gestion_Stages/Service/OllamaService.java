package com.OCP.Gestion_Stages.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class OllamaService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Configuration externalisée (application.yml) ──
    @Value("${app.ollama.url:http://localhost:11434/api/generate}")
    private String ollamaUrl;
    @Value("${app.ollama.model:llama3.2:3b}")
    private String model;
    @Value("${app.ollama.timeout-seconds:120}")
    private long timeoutSeconds;
    @Value("${app.ollama.temperature:0.2}")
    private double temperature;
    @Value("${app.ollama.embed-model:nomic-embed-text}")
    private String embedModel;

    // Connexion bornée : si Ollama est down, on échoue vite au lieu de bloquer le thread.
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // ════════════════════════════════════════════════════════════
    //  Appel central — JSON forcé, température basse, timeout
    // ════════════════════════════════════════════════════════════

    /**
     * Appelle Ollama. {@code jsonFormat=true} force une sortie JSON valide
     * (option native d'Ollama) → parsing fiable. Température basse = scores déterministes.
     */
    private String callOllama(String prompt, boolean jsonFormat) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("prompt", prompt);
        body.put("stream", false);
        body.put("options", Map.of("temperature", temperature, "num_predict", 800));
        if (jsonFormat) {
            body.put("format", "json");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaUrl))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama a répondu HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body()).path("response").asText("");
    }

    /** Vérifie rapidement si le service Ollama est joignable (pour les UI). */
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl.replace("/api/generate", "/api/tags")))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Use cases
    // ════════════════════════════════════════════════════════════

    /** Extraction structurée des infos d'un CV. */
    public String extraireInfosCV(String texteCV) {
        String prompt = """
            Tu es un assistant RH. Analyse ce CV et extrais les informations en JSON UNIQUEMENT.

            Format JSON attendu:
            {
              "nom": "...", "prenom": "...", "email": "...", "telephone": "...",
              "filiere": "...", "niveau": "...", "etablissement": "...",
              "departementSuggere": "...", "sujetStage": "..."
            }

            Pour departementSuggere, choisis parmi: Informatique, Finance, RH, Marketing, Production, Logistique
            Pour niveau, choisis parmi: Bac+2, Bac+3, Bac+4, Bac+5

            CV à analyser:
            %s
            """.formatted(texteCV);
        try {
            String response = callOllama(prompt, true);
            String json = extractJson(response);
            return json != null ? json : response;
        } catch (Exception e) {
            log.error("Erreur Ollama (extraction CV): {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'analyse du CV: " + e.getMessage());
        }
    }

    /** Score de matching CV ↔ offre (0-100). Renvoie 0 si IA indisponible. */
    public int calculerScoreMatching(String texteCV, String titreAnnonce,
                                     String descriptionAnnonce, String competencesRequises,
                                     String niveauRequis, String filiereRequise) {
        String prompt = """
            Tu es un expert RH. Évalue le matching entre ce CV et cette offre de stage.
            Réponds avec un JSON valide UNIQUEMENT.

            OFFRE DE STAGE:
            Titre: %s
            Description: %s
            Compétences requises: %s
            Niveau requis: %s
            Filière requise: %s

            CV DU CANDIDAT:
            %s

            JSON attendu:
            {
              "score": <nombre entre 0 et 100>,
              "competencesMatchees": ["..."],
              "competencesManquantes": ["..."],
              "pointsForts": "...",
              "pointsFaibles": "...",
              "recommendation": "RECOMMANDE" | "ACCEPTABLE" | "NON_RECOMMANDE"
            }
            """.formatted(titreAnnonce, descriptionAnnonce, competencesRequises,
                niveauRequis, filiereRequise, texteCV);
        try {
            JsonNode result = parseJson(callOllama(prompt, true));
            return result != null ? clampScore(safeInt(result, "score")) : 0;
        } catch (Exception e) {
            log.error("Erreur calcul matching: {}", e.getMessage());
            return 0;
        }
    }

    /** Analyse de matching complète (score + points forts/faibles + reco). */
    public Map<String, Object> analyserMatchingComplet(String texteCV, String titreAnnonce,
                                                       String descriptionAnnonce, String competencesRequises,
                                                       String niveauRequis, String filiereRequise) {
        String prompt = """
            Tu es un expert RH. Analyse ce CV par rapport à cette offre de stage.
            Réponds avec un JSON valide UNIQUEMENT.

            OFFRE DE STAGE:
            Titre: %s
            Description: %s
            Compétences requises: %s
            Niveau requis: %s
            Filière requise: %s

            CV DU CANDIDAT:
            %s

            JSON attendu:
            {
              "score": <0-100>,
              "competencesMatchees": ["..."],
              "competencesManquantes": ["..."],
              "pointsForts": "...",
              "pointsFaibles": "...",
              "recommendation": "RECOMMANDE" | "ACCEPTABLE" | "NON_RECOMMANDE"
            }
            """.formatted(titreAnnonce, descriptionAnnonce, competencesRequises,
                niveauRequis, filiereRequise, texteCV);
        try {
            JsonNode result = parseJson(callOllama(prompt, true));
            if (result == null) return Map.of("score", 0);
            Map<String, Object> map = new HashMap<>();
            map.put("score", clampScore(safeInt(result, "score")));
            map.put("pointsForts", safeText(result, "pointsForts"));
            map.put("pointsFaibles", safeText(result, "pointsFaibles"));
            map.put("recommendation", safeText(result, "recommendation"));
            return map;
        } catch (Exception e) {
            log.error("Erreur analyse matching: {}", e.getMessage());
            return Map.of("score", 0);
        }
    }

    /** Score détaillé par 6 critères (C1). Fallback neutre (50) si IA indisponible. */
    public Map<String, Object> calculerScoreMatchingDetaile(String texteCV, String specialite, String departement) {
        String prompt = """
            Tu es un expert RH chez OCP Group Maroc. Évalue la compatibilité du candidat sur 6 critères.

            Spécialité recherchée: %s
            Département OCP: %s

            Critères (score 0-100 chacun):
            1. Disponibilité  2. Proximité géographique  3. Compétences techniques
            4. Niveau d'études  5. Adéquation au sujet  6. Expériences antérieures

            JSON attendu UNIQUEMENT:
            {
              "scoreGlobal": <0-100>,
              "disponibilite": <0-100>, "proximiteGeographique": <0-100>,
              "competencesTechniques": <0-100>, "niveauEtudes": <0-100>,
              "adequationSujet": <0-100>, "experiencesAnterieures": <0-100>,
              "pointsForts": "...", "pointsFaibles": "...",
              "recommendation": "RECOMMANDE" | "ACCEPTABLE" | "NON_RECOMMANDE"
            }

            CV à analyser:
            %s
            """.formatted(specialite, departement, texteCV);
        try {
            JsonNode result = parseJson(callOllama(prompt, true));
            if (result == null) return Map.of("scoreGlobal", 50);
            Map<String, Object> map = new HashMap<>();
            for (String k : new String[]{"scoreGlobal", "disponibilite", "proximiteGeographique",
                    "competencesTechniques", "niveauEtudes", "adequationSujet", "experiencesAnterieures"}) {
                map.put(k, clampScore(safeInt(result, k)));
            }
            map.put("pointsForts", safeText(result, "pointsForts"));
            map.put("pointsFaibles", safeText(result, "pointsFaibles"));
            map.put("recommendation", safeText(result, "recommendation"));
            return map;
        } catch (Exception e) {
            log.warn("Erreur score IA détaillé: {}", e.getMessage());
            return Map.of("scoreGlobal", 50);
        }
    }

    /** Score de compatibilité par spécialité (nombre brut). Fallback 50. */
    public int calculerScoreMatchingSpecialite(String texteCV, String specialite, String departement) {
        String prompt = """
            Tu es un expert RH chez OCP Group Maroc. Calcule un score de compatibilité (0-100).

            Spécialité: %s
            Département: %s

            Barème: compétences techniques (40), projets/expériences (30),
            niveau d'études (20), cohérence du profil (10).

            Réponds UNIQUEMENT par un nombre entier entre 0 et 100, sans aucun texte.

            CV:
            %s
            """.formatted(specialite, departement, texteCV);
        try {
            // Pas de format JSON ici : on attend un nombre brut.
            String raw = callOllama(prompt, false).replaceAll("[^0-9]", "").trim();
            if (raw.isEmpty()) return 50;
            return clampScore(Integer.parseInt(raw.substring(0, Math.min(raw.length(), 3))));
        } catch (Exception e) {
            log.warn("Erreur score IA spécialité: {}", e.getMessage());
            return 50;
        }
    }

    // ── C2 : Vérification documents ──

    public Map<String, Object> verifierDocument(String texteExtrait, String typeDocument, String nomFichier) {
        String prompt = """
            Tu es un agent de contrôle documentaire chez OCP Group. Détermine si ce contenu
            correspond VRAIMENT à un document AUTHENTIQUE du type attendu.

            Type attendu: %s
            Nom du fichier: %s

            Contenu extrait:
            %s

            Éléments caractéristiques par type :
            - CV : nom/prénom, formation/diplôme, compétences, expériences ;
            - CIN : numéro CIN, nom/prénom, date de naissance, mentions officielles ;
            - LETTRE_MOTIVATION : destinataire, objet, corps motivé, signature ;
            - ASSURANCE : numéro de police, assuré, dates de validité, assureur.

            Sois STRICT : signale comme suspect un contenu vide, hors-sujet, incohérent,
            ou qui ne ressemble pas à un vrai document du type demandé.
            - typeCorrespond = true seulement si le contenu correspond bien au type attendu.
            - score reflète la ressemblance à un vrai document (0 = faux/vide, 100 = authentique complet).

            JSON attendu UNIQUEMENT:
            {
              "valide": true/false,
              "typeCorrespond": true/false,
              "score": <0-100>,
              "typeDetecte": "type réellement détecté",
              "elementsManquants": ["..."],
              "alertes": ["incohérence ou signe suspect"],
              "remarque": "verdict en une phrase"
            }
            """.formatted(typeDocument, nomFichier, texteExtrait);
        try {
            JsonNode result = parseJson(callOllama(prompt, true));
            if (result == null) return Map.of("valide", false, "score", 0, "typeCorrespond", false,
                    "elementsManquants", java.util.List.of(), "alertes", java.util.List.of(),
                    "remarque", "Impossible d'analyser le document");
            Map<String, Object> map = new HashMap<>();
            map.put("valide", result.path("valide").asBoolean(false));
            map.put("typeCorrespond", result.path("typeCorrespond").asBoolean(false));
            map.put("score", clampScore(safeInt(result, "score")));
            map.put("typeDetecte", safeText(result, "typeDetecte"));
            map.put("elementsManquants", safeList(result, "elementsManquants"));
            map.put("alertes", safeList(result, "alertes"));
            map.put("remarque", safeText(result, "remarque"));
            return map;
        } catch (Exception e) {
            log.warn("Erreur vérification document IA: {}", e.getMessage());
            return Map.of("valide", false, "score", 0, "typeCorrespond", false,
                    "elementsManquants", java.util.List.of(), "alertes", java.util.List.of(),
                    "remarque", "Service IA indisponible");
        }
    }

    public Map<String, Object> verifierCoherenceDossier(String texteCV, String texteCIN, String nomCandidat, String prenomCandidat) {
        String prompt = """
            Tu es un assistant RH. Vérifie la cohérence entre les documents d'un candidat.

            Nom attendu: %s
            Prénom attendu: %s

            Contenu CV:
            %s

            Contenu CIN:
            %s

            JSON attendu UNIQUEMENT:
            {
              "coherent": true/false, "scoreCoherence": <0-100>,
              "nomCorrespond": true/false, "alertes": ["..."], "remarque": "..."
            }
            """.formatted(nomCandidat, prenomCandidat, texteCV, texteCIN);
        try {
            JsonNode result = parseJson(callOllama(prompt, true));
            if (result == null) return Map.of("coherent", false, "scoreCoherence", 0);
            Map<String, Object> map = new HashMap<>();
            map.put("coherent", result.path("coherent").asBoolean(false));
            map.put("scoreCoherence", clampScore(safeInt(result, "scoreCoherence")));
            map.put("nomCorrespond", result.path("nomCorrespond").asBoolean(false));
            map.put("remarque", safeText(result, "remarque"));
            return map;
        } catch (Exception e) {
            log.warn("Erreur vérification cohérence IA: {}", e.getMessage());
            return Map.of("coherent", false, "scoreCoherence", 0);
        }
    }

    // ── C3 : Analyse performance stagiaire ──

    public Map<String, Object> analyserPerformanceStagiaire(double tauxPresence, double moyenneNotes,
                                                             String remarques, String appreciationGlobale) {
        String prompt = """
            Tu es un expert RH chez OCP Group. Analyse la performance de ce stagiaire en fin de stage.

            Données:
            - Taux de présence: %.1f%%
            - Moyenne des notes: %.1f/20
            - Remarques de l'encadrant: %s
            - Appréciation globale: %s

            JSON attendu UNIQUEMENT:
            {
              "scoreGlobal": <0-100>, "synthese": "...",
              "pointsForts": ["..."], "axesAmelioration": ["..."],
              "recommendation": "EXCELLENT" | "BIEN" | "SATISFAISANT" | "INSUFFISANT",
              "conseilCarriere": "...", "apteEmbauche": true/false
            }
            """.formatted(tauxPresence, moyenneNotes, remarques, appreciationGlobale);
        try {
            JsonNode result = parseJson(callOllama(prompt, true));
            if (result == null) return Map.of("scoreGlobal", 50, "synthese", "Analyse non disponible");
            Map<String, Object> map = new HashMap<>();
            map.put("scoreGlobal", clampScore(safeInt(result, "scoreGlobal")));
            map.put("synthese", safeText(result, "synthese"));
            map.put("recommendation", safeText(result, "recommendation"));
            map.put("conseilCarriere", safeText(result, "conseilCarriere"));
            map.put("apteEmbauche", result.path("apteEmbauche").asBoolean(false));
            if (result.has("pointsForts")) map.put("pointsForts", objectMapper.convertValue(result.get("pointsForts"), java.util.List.class));
            if (result.has("axesAmelioration")) map.put("axesAmelioration", objectMapper.convertValue(result.get("axesAmelioration"), java.util.List.class));
            return map;
        } catch (Exception e) {
            log.warn("Erreur analyse performance IA: {}", e.getMessage());
            return Map.of("scoreGlobal", 50, "synthese", "Service IA indisponible");
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Matching sémantique par EMBEDDINGS (vecteurs) + scoring explicable
    // ════════════════════════════════════════════════════════════

    /** Calcule l'embedding (vecteur) d'un texte via Ollama. Renvoie null si indisponible. */
    public float[] embed(String texte) {
        if (texte == null || texte.isBlank()) return null;
        try {
            String url = ollamaUrl.replace("/api/generate", "/api/embeddings");
            Map<String, Object> body = Map.of("model", embedModel, "prompt", texte.length() > 6000 ? texte.substring(0, 6000) : texte);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode arr = objectMapper.readTree(res.body()).path("embedding");
            if (!arr.isArray() || arr.isEmpty()) return null;
            float[] v = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) v[i] = (float) arr.get(i).asDouble();
            return v;
        } catch (Exception e) {
            log.warn("Erreur embedding: {}", e.getMessage());
            return null;
        }
    }

    /** Similarité cosinus entre deux vecteurs (0 à 1). */
    public double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i]; }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * Matching CV ↔ poste EXPLICABLE : combine la similarité sémantique (embeddings),
     * la couverture des compétences requises et l'adéquation du niveau, avec des poids transparents.
     */
    public Map<String, Object> matchingSemantique(String texteCV, String descriptionPoste,
                                                   String competencesRequises, String niveauRequis) {
        Map<String, Object> r = new HashMap<>();
        String cv = texteCV == null ? "" : texteCV.toLowerCase();

        // 1) Similarité sémantique (cosinus des embeddings → 0-100)
        float[] eCv = embed(texteCV);
        float[] ePoste = embed((descriptionPoste == null ? "" : descriptionPoste) + " "
                + (competencesRequises == null ? "" : competencesRequises));
        int scoreSemantique = (int) Math.round(Math.max(0, Math.min(1, cosineSimilarity(eCv, ePoste))) * 100);

        // 2) Couverture des compétences requises (présence dans le CV)
        java.util.List<String> trouvees = new java.util.ArrayList<>();
        java.util.List<String> manquantes = new java.util.ArrayList<>();
        if (competencesRequises != null && !competencesRequises.isBlank()) {
            for (String comp : competencesRequises.split("[,;/]")) {
                String c = comp.trim().toLowerCase();
                if (c.isEmpty()) continue;
                if (cv.contains(c)) trouvees.add(comp.trim()); else manquantes.add(comp.trim());
            }
        }
        int totalComp = trouvees.size() + manquantes.size();
        int scoreCompetences = totalComp == 0 ? scoreSemantique : (int) Math.round(100.0 * trouvees.size() / totalComp);

        // 3) Adéquation du niveau
        int scoreNiveau = (niveauRequis != null && !niveauRequis.isBlank()
                && cv.contains(niveauRequis.toLowerCase())) ? 100 : 60;

        // Score global pondéré (transparent)
        int scoreGlobal = (int) Math.round(0.50 * scoreSemantique + 0.35 * scoreCompetences + 0.15 * scoreNiveau);

        r.put("scoreGlobal", clampScore(scoreGlobal));
        r.put("scoreSemantique", clampScore(scoreSemantique));
        r.put("scoreCompetences", clampScore(scoreCompetences));
        r.put("scoreNiveau", clampScore(scoreNiveau));
        r.put("competencesTrouvees", trouvees);
        r.put("competencesManquantes", manquantes);
        r.put("methode", (eCv != null && ePoste != null) ? "embeddings" : "fallback-mots-cles");
        return r;
    }

    // ── Helpers ──

    private JsonNode parseJson(String text) {
        try {
            String json = extractJson(text);
            return json != null ? objectMapper.readTree(json) : null;
        } catch (Exception e) {
            log.warn("Réponse IA non parsable: {}", e.getMessage());
            return null;
        }
    }

    private String extractJson(String text) {
        if (text == null) return null;
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}') + 1;
        return (start >= 0 && end > start) ? text.substring(start, end) : null;
    }

    private int safeInt(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asInt(0) : 0;
    }

    private String safeText(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText("") : "";
    }

    private java.util.List<String> safeList(JsonNode node, String field) {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (node.has(field) && node.get(field).isArray()) {
            node.get(field).forEach(n -> list.add(n.asText("")));
        }
        return list;
    }

    private int clampScore(int score) {
        return Math.min(100, Math.max(0, score));
    }
}
