package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.EmailService;
import com.OCP.Gestion_Stages.Service.interfaces.RapportStageService;
import com.OCP.Gestion_Stages.domain.dto.rapport.RapportResponse;
import com.OCP.Gestion_Stages.domain.model.*;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
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

    @Override
    public RapportResponse upload(Long stageId, MultipartFile file, String username) throws IOException {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable : " + stageId));

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

    private RapportResponse toResponse(RapportStage r) {
        RapportResponse res = new RapportResponse();
        res.setId(r.getId());
        res.setNomFichier(r.getNomFichier());
        res.setTypeContenu(r.getTypeContenu());
        res.setTaille(r.getTaille());
        res.setUploadedAt(r.getUploadedAt());
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