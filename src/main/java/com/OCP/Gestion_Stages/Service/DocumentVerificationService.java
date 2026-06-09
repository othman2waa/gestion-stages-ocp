package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.Repository.DocumentCandidatureRepository;
import com.OCP.Gestion_Stages.domain.model.DocumentCandidature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Vérification IA d'un document candidature, déclenchée à l'upload (côté stagiaire) en asynchrone.
 * - Images (PHOTO, jpg/png…) : contrôle léger sans LLM (un modèle texte ne « voit » pas l'image).
 * - PDF/texte : extraction + Ollama (ressemble-t-il à un vrai document du type attendu ?).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentVerificationService {

    private final DocumentCandidatureRepository documentRepository;
    private final OllamaService ollamaService;
    private final FileStorageService fileStorageService;

    @Value("${app.ocr.tessdata-path:/usr/local/share/tessdata}")
    private String tessdataPath;
    @Value("${app.ocr.languages:fra+eng}")
    private String ocrLanguages;

    /** Vérifie le document en arrière-plan (l'upload répond immédiatement). */
    @Async
    public void verifierAsync(Long documentId) {
        documentRepository.findById(documentId).ifPresent(this::verifier);
    }

    /** Vérifie un document (synchrone), met à jour statut/score/commentaire et sauvegarde. Renvoie le score. */
    public int verifier(DocumentCandidature doc) {
        String type = doc.getTypeDocument();
        byte[] bytes = lireBytes(doc);
        if (bytes == null || bytes.length == 0) {
            return appliquer(doc, "SUSPECT", 0, "Document " + type + " : aucun fichier reçu.");
        }

        // Extraction du texte : PDF (PDFBox) OU image (OCR Tesseract)
        String texte = extraireTexte(doc);

        if (!texte.isBlank()) {
            // Analyse du contenu par Ollama (vaut aussi pour les images OCRisées)
            Map<String, Object> ia = ollamaService.verifierDocument(tronquer(texte, 2000), type, doc.getNomFichier());
            int score = ((Number) ia.getOrDefault("score", 0)).intValue();
            boolean typeOk = Boolean.TRUE.equals(ia.get("typeCorrespond"));
            boolean valide = Boolean.TRUE.equals(ia.get("valide"));
            @SuppressWarnings("unchecked")
            List<String> manquants = (List<String>) ia.getOrDefault("elementsManquants", List.of());
            @SuppressWarnings("unchecked")
            List<String> alertes = (List<String>) ia.getOrDefault("alertes", List.of());

            StringBuilder sb = new StringBuilder("Document " + type + " : ");
            sb.append(String.valueOf(ia.getOrDefault("remarque", "")));
            if (!typeOk) sb.append(" ⚠ Ne ressemble pas à un vrai ").append(type).append(".");
            if (manquants != null && !manquants.isEmpty()) sb.append(" Manque : ").append(String.join(", ", manquants)).append(".");
            if (alertes != null && !alertes.isEmpty()) sb.append(" Alertes : ").append(String.join(", ", alertes)).append(".");

            String statut = (valide && typeOk) || score >= 80 ? "VALIDE" : "SUSPECT";
            return appliquer(doc, statut, score, sb.toString());
        }

        // Pas de texte exploitable (OCR vide) : image valide ? sinon non analysable
        String nom = doc.getNomFichier() != null ? doc.getNomFichier().toLowerCase() : "";
        if (estImage(type, nom) && imageValide(bytes)) {
            return appliquer(doc, "VALIDE", 80, "Document " + type + " : image valide (aucun texte extrait par OCR).");
        }
        return appliquer(doc, "NON_ANALYSE", 60, "Document " + type + " : contenu non analysable.");
    }

    /** Extrait le texte d'un document (lecture disque/bytea + PDFBox si PDF). Public pour réutilisation (cohérence). */
    public String extraireTexte(DocumentCandidature doc) {
        try {
            byte[] bytes = lireBytes(doc);
            if (bytes == null || bytes.length == 0) return "";
            String nom = doc.getNomFichier() != null ? doc.getNomFichier().toLowerCase() : "";
            if (nom.endsWith(".pdf")) {
                try (org.apache.pdfbox.pdmodel.PDDocument pdf = org.apache.pdfbox.Loader.loadPDF(bytes)) {
                    return new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);
                }
            }
            if (estImage(doc.getTypeDocument(), nom)) {
                return ocr(bytes); // OCR Tesseract pour les images (CIN, photo…)
            }
            return "";
        } catch (Exception e) {
            log.warn("Extraction texte document {} échouée: {}", doc.getId(), e.getMessage());
            return "";
        }
    }

    /** OCR d'une image via Tesseract (Tess4J). Dégrade proprement si l'OCR est indisponible. */
    private String ocr(byte[] bytes) {
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            if (img == null) return "";
            net.sourceforge.tess4j.Tesseract t = new net.sourceforge.tess4j.Tesseract();
            t.setDatapath(tessdataPath);
            t.setLanguage(ocrLanguages);
            String texte = t.doOCR(img);
            return texte != null ? texte.trim() : "";
        } catch (Throwable e) { // Throwable : capture aussi les erreurs de chargement natif (UnsatisfiedLinkError)
            log.warn("OCR indisponible/échoué: {}", e.getMessage());
            return "";
        }
    }

    public String tronquer(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    // ── Helpers ──

    private int appliquer(DocumentCandidature doc, String statut, int score, String commentaire) {
        doc.setStatutIa(statut);
        doc.setScoreIa(score);
        doc.setCommentaireIa(commentaire);
        documentRepository.save(doc);
        return score;
    }

    private byte[] lireBytes(DocumentCandidature doc) {
        try {
            return doc.getCheminFichier() != null ? fileStorageService.read(doc.getCheminFichier()) : doc.getContenu();
        } catch (Exception e) {
            log.warn("Lecture fichier document {} échouée: {}", doc.getId(), e.getMessage());
            return null;
        }
    }

    private boolean estImage(String type, String nom) {
        return "PHOTO".equalsIgnoreCase(type)
                || nom.endsWith(".jpg") || nom.endsWith(".jpeg") || nom.endsWith(".png")
                || nom.endsWith(".gif") || nom.endsWith(".webp") || nom.endsWith(".bmp");
    }

    /** Vérifie les "magic bytes" d'une image courante. */
    private boolean imageValide(byte[] b) {
        if (b.length < 4) return false;
        // JPEG FF D8 FF
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return true;
        // PNG 89 50 4E 47
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return true;
        // GIF8
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return true;
        // RIFF (WEBP) / BM (BMP)
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F') return true;
        if (b[0] == 'B' && b[1] == 'M') return true;
        return false;
    }
}
