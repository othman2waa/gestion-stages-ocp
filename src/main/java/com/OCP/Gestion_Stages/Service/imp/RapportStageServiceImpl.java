package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.EmailService;
import com.OCP.Gestion_Stages.Service.interfaces.RapportStageService;
import com.OCP.Gestion_Stages.domain.dto.rapport.RapportResponse;
import com.OCP.Gestion_Stages.domain.model.*;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import com.OCP.Gestion_Stages.exeptions.BusinessException;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RapportStageServiceImpl implements RapportStageService {

    private final RapportStageRepository rapportRepository;
    private final StageRepository stageRepository;
    private final StagiaireRepository stagiaireRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final DocumentStagiaireRepository documentStagiaireRepository;
    private final com.OCP.Gestion_Stages.Service.FileStorageService fileStorageService;
    private final com.OCP.Gestion_Stages.Service.OllamaService ollamaService;
    private final com.OCP.Gestion_Stages.Service.interfaces.NotificationService notificationService;

    @Override
    public RapportResponse upload(Long stageId, MultipartFile file, String username) throws IOException {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable : " + stageId));

        // Garde-fou métier : un rapport ne se dépose que sur un stage en cours ou en attente d'évaluation
        if (stage.getStatut() != StageStatus.EN_COURS
                && stage.getStatut() != StageStatus.EN_ATTENTE_EVALUATION)
            throw new BusinessException("Le rapport ne peut être déposé que pour un stage en cours "
                    + "(état actuel : « " + stage.getStatut().libelle() + " »).");

        // Remplace l'ancien rapport si existant
        rapportRepository.findByStageId(stageId).ifPresent(rapportRepository::delete);

        RapportStage rapport = new RapportStage();
        rapport.setStage(stage);
        rapport.setNomFichier(file.getOriginalFilename());
        rapport.setTypeContenu(file.getContentType());
        rapport.setTaille(file.getSize());
        rapport.setContenu(file.getBytes());

        RapportStage saved = rapportRepository.save(rapport);

        // Notifier encadrant par email
        try {
            if (stage.getEncadrant() != null && stage.getEncadrant().getEmail() != null) {
                String sujet = "📄 Nouveau rapport de stage — " +
                        (stage.getStagiaire() != null
                                ? stage.getStagiaire().getPrenom() + " " + stage.getStagiaire().getNom()
                                : "");
                String contenu = """
                    <html><body style="font-family:Arial,sans-serif">
                    <div style="max-width:600px;margin:auto;padding:20px;border:1px solid #e2e8f0;border-radius:8px">
                    <div style="background:#00843D;padding:16px;border-radius:6px 6px 0 0;text-align:center">
                      <h2 style="color:white;margin:0">OCP — Nouveau rapport de stage</h2>
                    </div>
                    <div style="padding:20px">
                      <p>Bonjour <strong>%s %s</strong>,</p>
                      <p>Le stagiaire <strong>%s %s</strong> a déposé son rapport de stage.</p>
                      <p><b>Stage :</b> %s</p>
                      <p><b>Fichier :</b> %s</p>
                      <p style="margin-top:16px">
                        <a href="http://localhost:4200"
                           style="background:#00843D;color:white;padding:10px 20px;border-radius:6px;text-decoration:none">
                           Accéder à la plateforme
                        </a>
                      </p>
                    </div></div></body></html>
                    """.formatted(
                        stage.getEncadrant().getPrenom(), stage.getEncadrant().getNom(),
                        stage.getStagiaire() != null ? stage.getStagiaire().getPrenom() : "",
                        stage.getStagiaire() != null ? stage.getStagiaire().getNom() : "",
                        stage.getSujet(),
                        file.getOriginalFilename()
                );
                emailService.envoyerEmail(stage.getEncadrant().getEmail(), sujet, contenu);
            }
        } catch (Exception e) {
            log.warn("Email encadrant non envoyé : {}", e.getMessage());
        }

        // Notification in-app à l'encadrant
        try {
            if (stage.getEncadrant() != null && stage.getEncadrant().getUser() != null) {
                String candidat = stage.getStagiaire() != null
                        ? stage.getStagiaire().getPrenom() + " " + stage.getStagiaire().getNom() : "Un stagiaire";
                notificationService.notifierSysteme(stage.getEncadrant().getUser().getId(),
                        "Nouveau rapport de stage",
                        candidat + " a déposé son rapport pour « " + stage.getSujet() + " ».");
            }
        } catch (Exception e) {
            log.warn("Notification dépôt rapport non créée : {}", e.getMessage());
        }

        return toResponse(saved);
    }

    @Override
    public RapportStage download(Long stageId) {
        return rapportRepository.findByStageId(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport introuvable pour le stage : " + stageId));
    }

    @Override
    public RapportResponse getMeta(Long stageId) {
        RapportStage rapport = rapportRepository.findByStageId(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport introuvable"));
        return toResponse(rapport);
    }

    @Override
    public void delete(Long stageId) {
        RapportStage rapport = rapportRepository.findByStageId(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport introuvable"));
        rapportRepository.delete(rapport);
    }

    @Override
    public List<RapportResponse> getMesRapports(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Stagiaire stagiaire = stagiaireRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire introuvable"));
        List<Stage> stages = stageRepository.findByStagiaireId(stagiaire.getId());
        return stages.stream()
                .map(s -> rapportRepository.findByStageId(s.getId()))
                .filter(java.util.Optional::isPresent)
                .map(opt -> toResponse(opt.get()))
                .collect(Collectors.toList());
    }

    @Override
    public RapportResponse valider(Long stageId, String decision, String commentaire, String username) {
        RapportStage rapport = rapportRepository.findByStageId(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport introuvable pour le stage : " + stageId));

        boolean valide = "VALIDE".equalsIgnoreCase(decision) || "VALIDER".equalsIgnoreCase(decision);
        rapport.setStatut(valide ? "VALIDE" : "REFUSE");
        rapport.setCommentaireValidation(commentaire);
        rapport.setValideAt(java.time.LocalDateTime.now());
        RapportStage saved = rapportRepository.save(rapport);

        // Archivage dans les documents du stagiaire — uniquement si validé
        if (valide) archiverDansDocuments(saved);

        // Notifier le stagiaire du résultat
        try {
            Stage stage = saved.getStage();
            if (stage != null && stage.getStagiaire() != null && stage.getStagiaire().getEmail() != null) {
                String corps = """
                    <html><body style="font-family:Arial,sans-serif">
                    <div style="max-width:600px;margin:auto;padding:20px;border:1px solid #e2e8f0;border-radius:8px">
                    <div style="background:%s;padding:16px;border-radius:6px 6px 0 0;text-align:center">
                      <h2 style="color:white;margin:0">OCP — %s</h2>
                    </div>
                    <div style="padding:20px">
                      <p>Bonjour <strong>%s %s</strong>,</p>
                      <p>Votre rapport de stage a été <strong>%s</strong> par votre encadrant.</p>
                      %s
                    </div></div></body></html>
                    """.formatted(
                        valide ? "#00843D" : "#DC2626",
                        valide ? "Rapport validé" : "Rapport à corriger",
                        stage.getStagiaire().getPrenom(), stage.getStagiaire().getNom(),
                        valide ? "validé" : "refusé",
                        commentaire != null && !commentaire.isBlank()
                            ? "<p style='background:#f8fafc;border-radius:8px;padding:12px'><b>Commentaire :</b> " + commentaire + "</p>"
                            : ""
                );
                emailService.envoyerEmail(stage.getStagiaire().getEmail(),
                        valide ? "✅ Rapport de stage validé" : "❌ Rapport de stage à corriger", corps);
            }
        } catch (Exception e) {
            log.warn("Email validation rapport non envoyé : {}", e.getMessage());
        }

        // Notification in-app au stagiaire
        try {
            Stage stage = saved.getStage();
            if (stage != null && stage.getStagiaire() != null && stage.getStagiaire().getUser() != null) {
                notificationService.notifierSysteme(stage.getStagiaire().getUser().getId(),
                        valide ? "Rapport de stage validé" : "Rapport de stage à corriger",
                        valide ? "Votre encadrant a validé votre rapport."
                               : "Votre encadrant a demandé une correction de votre rapport.");
            }
        } catch (Exception e) {
            log.warn("Notification validation rapport non créée : {}", e.getMessage());
        }

        return toResponse(saved);
    }

    @Override
    public String resumer(Long stageId) {
        RapportStage rapport = rapportRepository.findByStageId(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport introuvable pour le stage : " + stageId));
        String texte = extraireTextePdf(rapport.getContenu());
        return ollamaService.resumerRapport(texte);
    }

    /** Extrait le texte d'un PDF (rapport) via PDFBox ; renvoie "" si illisible. */
    private String extraireTextePdf(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        try (org.apache.pdfbox.pdmodel.PDDocument pdf = org.apache.pdfbox.Loader.loadPDF(bytes)) {
            return new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);
        } catch (Exception e) {
            log.warn("Extraction texte rapport échouée : {}", e.getMessage());
            return "";
        }
    }

    /** Archive le rapport validé parmi les documents du stagiaire (type RAPPORT, un seul). */
    private void archiverDansDocuments(RapportStage rapport) {
        try {
            Stagiaire stagiaire = rapport.getStage() != null ? rapport.getStage().getStagiaire() : null;
            if (stagiaire == null || rapport.getContenu() == null) return;
            documentStagiaireRepository.findByStagiaireIdAndTypeDocument(stagiaire.getId(), "RAPPORT")
                    .ifPresent(existing -> {
                        if (existing.getCheminFichier() != null) fileStorageService.delete(existing.getCheminFichier());
                        documentStagiaireRepository.delete(existing);
                    });
            String chemin = fileStorageService.store(rapport.getContenu(), rapport.getNomFichier());
            DocumentStagiaire doc = DocumentStagiaire.builder()
                    .stagiaire(stagiaire)
                    .typeDocument("RAPPORT")
                    .nomFichier(rapport.getNomFichier())
                    .contentType(rapport.getTypeContenu())
                    .taille(rapport.getTaille())
                    .cheminFichier(chemin)
                    .contenu(rapport.getContenu()) // colonne legacy NOT NULL
                    .build();
            documentStagiaireRepository.save(doc);
        } catch (Exception e) {
            log.warn("Archivage rapport dans documents échoué : {}", e.getMessage());
        }
    }

    private RapportResponse toResponse(RapportStage r) {
        RapportResponse res = new RapportResponse();
        res.setId(r.getId());
        res.setNomFichier(r.getNomFichier());
        res.setTypeContenu(r.getTypeContenu());
        res.setTaille(r.getTaille());
        res.setUploadedAt(r.getUploadedAt());
        res.setStatut(r.getStatut());
        res.setCommentaireValidation(r.getCommentaireValidation());
        res.setValideAt(r.getValideAt());
        if (r.getStage() != null) {
            res.setStageId(r.getStage().getId());
            res.setStageSujet(r.getStage().getSujet());
            if (r.getStage().getStagiaire() != null)
                res.setStagiaireNom(r.getStage().getStagiaire().getPrenom()
                        + " " + r.getStage().getStagiaire().getNom());
        }
        return res;
    }
}