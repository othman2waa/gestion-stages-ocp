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
    @Value("${app.ollama.timeout-seconds:45}")
    private long timeoutSeconds;
    @Value("${app.ollama.temperature:0.2}")
    private double temperature;

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
            Tu es un assistant RH chez OCP Group. Vérifie ce document et évalue sa validité.

            Type attendu: %s
            Nom du fichier: %s

            Contenu extrait:
            %s

            Vérifie selon le type (CV: nom/formation/compétences ; CIN: numéro/nom/date ;
            LETTRE_MOTIVATION: destinataire/objet ; ASSURANCE: police/dates).

            JSON attendu UNIQUEMENT:
            {
              "valide": true/false, "score": <0-100>, "typeDetecte": "...",
              "elementsPresents": ["..."], "elementsManquants": ["..."],
              "alertes": ["..."], "remarque": "..."
            }
            """.formatted(typeDocument, nomFichier, texteExtrait);
        try {
            JsonNode result = parseJson(callOllama(prompt, true));
            if (result == null) return Map.of("valide", false, "score", 0, "remarque", "Impossible d'analyser le document");
            Map<String, Object> map = new HashMap<>();
            map.put("valide", result.path("valide").asBoolean(false));
            map.put("score", clampScore(safeInt(result, "score")));
            map.put("typeDetecte", safeText(result, "typeDetecte"));
            map.put("remarque", safeText(result, "remarque"));
            return map;
        } catch (Exception e) {
            log.warn("Erreur vérification document IA: {}", e.getMessage());
            return Map.of("valide", false, "score", 0, "remarque", "Service IA indisponible");
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

    private int clampScore(int score) {
        return Math.min(100, Math.max(0, score));
    }
}
