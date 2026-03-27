package com.OCP.Gestion_Stages.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
@Slf4j
public class OllamaService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "llama3.2:3b";

    public String extraireInfosCV(String texteCV) {
        String prompt = """
            Tu es un assistant RH. Analyse ce CV et extrais les informations en JSON UNIQUEMENT.
            Réponds SEULEMENT avec un objet JSON valide, aucun texte avant ou après.
            
            Format JSON attendu:
            {
              "nom": "...",
              "prenom": "...",
              "email": "...",
              "telephone": "...",
              "filiere": "...",
              "niveau": "...",
              "etablissement": "...",
              "departementSuggere": "...",
              "sujetStage": "..."
            }
            
            Pour departementSuggere, choisis parmi: Informatique, Finance, RH, Marketing, Production, Logistique
            Pour niveau, choisis parmi: Bac+2, Bac+3, Bac+4, Bac+5
            
            CV à analyser:
            %s
            """.formatted(texteCV);

        try {
            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "prompt", prompt,
                    "stream", false
            );

            String requestBody = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String responseText = root.get("response").asText();

            // Extraire le JSON de la réponse
            int start = responseText.indexOf('{');
            int end = responseText.lastIndexOf('}') + 1;
            if (start >= 0 && end > start) {
                return responseText.substring(start, end);
            }
            return responseText;

        } catch (Exception e) {
            log.error("Erreur Ollama: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'analyse du CV: " + e.getMessage());
        }
    }
    public int calculerScoreMatching(String texteCV, String titreAnnonce,
                                     String descriptionAnnonce, String competencesRequises,
                                     String niveauRequis, String filiereRequise) {
        String prompt = """
        Tu es un expert RH. Analyse ce CV par rapport à cette offre de stage et donne un score de matching.
        Réponds SEULEMENT avec un objet JSON valide.
        
        OFFRE DE STAGE:
        Titre: %s
        Description: %s
        Compétences requises: %s
        Niveau requis: %s
        Filière requise: %s
        
        CV DU CANDIDAT:
        %s
        
        Évalue le matching et réponds avec ce JSON UNIQUEMENT:
        {
          "score": <nombre entre 0 et 100>,
          "competencesMatchees": ["comp1", "comp2"],
          "competencesManquantes": ["comp3"],
          "pointsForts": "...",
          "pointsFaibles": "...",
          "recommendation": "RECOMMANDE" ou "ACCEPTABLE" ou "NON_RECOMMANDE"
        }
        """.formatted(titreAnnonce, descriptionAnnonce, competencesRequises,
                niveauRequis, filiereRequise, texteCV);

        try {
            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "prompt", prompt,
                    "stream", false
            );
            String requestBody = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String responseText = root.get("response").asText();

            int start = responseText.indexOf('{');
            int end = responseText.lastIndexOf('}') + 1;
            if (start >= 0 && end > start) {
                String jsonStr = responseText.substring(start, end);
                JsonNode result = objectMapper.readTree(jsonStr);
                return result.has("score") ? result.get("score").asInt() : 0;
            }
            return 0;
        } catch (Exception e) {
            log.error("Erreur calcul matching: {}", e.getMessage());
            return 0;
        }
    }

    public Map<String, Object> analyserMatchingComplet(String texteCV, String titreAnnonce,
                                                       String descriptionAnnonce, String competencesRequises,
                                                       String niveauRequis, String filiereRequise) {
        String prompt = """
        Tu es un expert RH. Analyse ce CV par rapport à cette offre de stage.
        Réponds SEULEMENT avec un objet JSON valide.
        
        OFFRE DE STAGE:
        Titre: %s
        Description: %s
        Compétences requises: %s
        Niveau requis: %s
        Filière requise: %s
        
        CV DU CANDIDAT:
        %s
        
        Réponds avec ce JSON UNIQUEMENT:
        {
          "score": <nombre entre 0 et 100>,
          "competencesMatchees": ["comp1", "comp2"],
          "competencesManquantes": ["comp3"],
          "pointsForts": "...",
          "pointsFaibles": "...",
          "recommendation": "RECOMMANDE" ou "ACCEPTABLE" ou "NON_RECOMMANDE"
        }
        """.formatted(titreAnnonce, descriptionAnnonce, competencesRequises,
                niveauRequis, filiereRequise, texteCV);

        try {
            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "prompt", prompt,
                    "stream", false
            );
            String requestBody = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String responseText = root.get("response").asText();

            int start = responseText.indexOf('{');
            int end = responseText.lastIndexOf('}') + 1;
            if (start >= 0 && end > start) {
                String jsonStr = responseText.substring(start, end);
                JsonNode result = objectMapper.readTree(jsonStr);
                Map<String, Object> resultMap = new java.util.HashMap<>();
                resultMap.put("score", result.has("score") ? result.get("score").asInt() : 0);
                resultMap.put("pointsForts", result.has("pointsForts") ? result.get("pointsForts").asText() : "");
                resultMap.put("pointsFaibles", result.has("pointsFaibles") ? result.get("pointsFaibles").asText() : "");
                resultMap.put("recommendation", result.has("recommendation") ? result.get("recommendation").asText() : "");
                return resultMap;
            }
            return Map.of("score", 0);
        } catch (Exception e) {
            log.error("Erreur analyse matching: {}", e.getMessage());
            return Map.of("score", 0);
        }
    }
}