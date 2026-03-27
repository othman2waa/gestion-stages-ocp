package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.EmailService;
import com.OCP.Gestion_Stages.Service.OllamaService;
import com.OCP.Gestion_Stages.Service.interfaces.CandidatureService;
import com.OCP.Gestion_Stages.domain.dto.candidature.*;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.enums.TypeStage;
import com.OCP.Gestion_Stages.domain.enums.UserRole;
import com.OCP.Gestion_Stages.domain.model.*;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CandidatureServiceImpl implements CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final UserRepository userRepository;
    private final StagiaireRepository stagiaireRepository;
    private final StageRepository stageRepository;
    private final EncadrantRepository encadrantRepository;
    private final DepartementRepository departementRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AnnonceStageRepository annonceRepository;
    private final OllamaService ollamaService;


    @Override
    public CandidatureResponse soumettre(CandidatureRequest request, MultipartFile cv) throws IOException {
        Candidature c = new Candidature();
        c.setNom(request.getNom());
        c.setPrenom(request.getPrenom());
        c.setEmail(request.getEmail());
        c.setTelephone(request.getTelephone());
        c.setFiliere(request.getFiliere());
        c.setNiveau(request.getNiveau());
        c.setEtablissement(request.getEtablissement());
        c.setSujetSouhaite(request.getSujetSouhaite());
        c.setDepartementSouhaite(request.getDepartementSouhaite());
        c.setMessage(request.getMessage());
        c.setStatut("EN_ATTENTE");
        if (request.getAnnonceId() != null) {
            c.setAnnonceId(request.getAnnonceId());
            // Calcul score matching IA si CV fourni
            if (cv != null && !cv.isEmpty()) {
                try {
                    annonceRepository.findById(request.getAnnonceId()).ifPresent(annonce -> {
                        try {
                            org.apache.pdfbox.pdmodel.PDDocument doc =
                                    org.apache.pdfbox.Loader.loadPDF(cv.getBytes());
                            org.apache.pdfbox.text.PDFTextStripper stripper =
                                    new org.apache.pdfbox.text.PDFTextStripper();
                            String texteCV = stripper.getText(doc);
                            doc.close();

                            int score = ollamaService.calculerScoreMatching(
                                    texteCV,
                                    annonce.getTitre(),
                                    annonce.getDescription(),
                                    annonce.getCompetencesRequises(),
                                    annonce.getNiveauRequis(),
                                    annonce.getFiliereRequise()
                            );
                            c.setScoreMatching(score);
                        } catch (Exception e) {
                            log.warn("Erreur calcul score: {}", e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    log.warn("Erreur lecture CV pour matching: {}", e.getMessage());
                }
            }
        }
        if (cv != null && !cv.isEmpty()) {
            c.setCvContenu(cv.getBytes());
            c.setCvNomFichier(cv.getOriginalFilename());
        }
        Candidature saved = candidatureRepository.save(c);
        // Notifier RH
        notifierRhNouvelleCandidature(saved);
        return toResponse(saved);
    }

    @Override
    public CandidatureResponse traiter(Long id, TraiterCandidatureRequest request, String username) throws Exception {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable : " + id));

        c.setStatut(request.getStatut());
        c.setCommentaireRh(request.getCommentaireRh());
        c.setTraiteAt(LocalDateTime.now());
        c.setTraitePar(username);

        if ("ACCEPTEE".equals(request.getStatut())) {
            creerCompteStagiaire(c, request);
        }

        candidatureRepository.save(c);
        notifierStagiaire(c);
        return toResponse(c);
    }

    private void creerCompteStagiaire(Candidature c, TraiterCandidatureRequest request) {
        String username = (c.getPrenom().toLowerCase() + "." + c.getNom().toLowerCase())
                .replaceAll("[^a-z.]", "");
        String password = "OCP@" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        User user = User.builder()
                .username(username).email(c.getEmail())
                .password(passwordEncoder.encode(password))
                .role(UserRole.STAGIAIRE).actif(true).build();
        userRepository.save(user);

        Stagiaire stagiaire = new Stagiaire();
        stagiaire.setNom(c.getNom());
        stagiaire.setPrenom(c.getPrenom());
        stagiaire.setEmail(c.getEmail());
        stagiaire.setTelephone(c.getTelephone());
        stagiaire.setFiliere(c.getFiliere());
        stagiaire.setNiveau(c.getNiveau());
        stagiaire.setUser(user);
        stagiaireRepository.save(stagiaire);

        // Créer stage
        Stage stage = new Stage();
        stage.setStagiaire(stagiaire);
        stage.setSujet(c.getSujetSouhaite() != null ? c.getSujetSouhaite() : "Stage PFE");
        stage.setStatut(StageStatus.VALIDEE);
        stage.setTypeStage(TypeStage.PFE);
        if (request.getEncadrantId() != null)
            encadrantRepository.findById(request.getEncadrantId()).ifPresent(stage::setEncadrant);
        if (request.getDepartementId() != null)
            departementRepository.findById(request.getDepartementId()).ifPresent(stage::setDepartement);
        stageRepository.save(stage);

        // Email credentials
        envoyerEmailAcceptation(c.getEmail(), c.getPrenom() + " " + c.getNom(), username, password);
    }

    private void notifierRhNouvelleCandidature(Candidature c) {
        try {
            String sujet = "🆕 Nouvelle candidature — " + c.getPrenom() + " " + c.getNom();
            String contenu = """
                <html><body style="font-family:Arial,sans-serif">
                <div style="max-width:600px;margin:auto;padding:20px;border:1px solid #e2e8f0;border-radius:8px">
                <div style="background:#00843D;padding:16px;border-radius:6px 6px 0 0;text-align:center">
                  <h2 style="color:white;margin:0">OCP — Nouvelle Candidature</h2>
                </div>
                <div style="padding:20px">
                  <h3>%s %s</h3>
                  <p><b>Email:</b> %s</p>
                  <p><b>Filière:</b> %s — %s</p>
                  <p><b>Établissement:</b> %s</p>
                  <p><b>Sujet souhaité:</b> %s</p>
                  <p><b>Département:</b> %s</p>
                  <p style="margin-top:16px"><a href="http://localhost:4200/candidatures" 
                     style="background:#00843D;color:white;padding:10px 20px;border-radius:6px;text-decoration:none">
                     Traiter la candidature</a></p>
                </div></div></body></html>
                """.formatted(c.getPrenom(), c.getNom(), c.getEmail(),
                    c.getFiliere(), c.getNiveau(), c.getEtablissement(),
                    c.getSujetSouhaite(), c.getDepartementSouhaite());
            emailService.envoyerEmail("admin@ocp.ma", sujet, contenu);
        } catch (Exception e) {
            log.warn("Email RH non envoyé : {}", e.getMessage());
        }
    }

    private void notifierStagiaire(Candidature c) {
        try {
            boolean accepte = "ACCEPTEE".equals(c.getStatut());
            String sujet = accepte
                    ? "✅ Votre candidature OCP a été acceptée"
                    : "❌ Votre candidature OCP";
            String couleur = accepte ? "#00843D" : "#dc2626";
            String message = accepte
                    ? "Félicitations ! Votre candidature a été acceptée. Vos identifiants de connexion vous ont été envoyés."
                    : "Nous avons bien étudié votre candidature mais ne pouvons pas y donner suite pour le moment.";
            String contenu = """
                <html><body style="font-family:Arial,sans-serif">
                <div style="max-width:600px;margin:auto;padding:20px;border:1px solid #e2e8f0;border-radius:8px">
                <div style="background:%s;padding:16px;border-radius:6px 6px 0 0;text-align:center">
                  <h2 style="color:white;margin:0">OCP — Réponse à votre candidature</h2>
                </div>
                <div style="padding:20px">
                  <p>Bonjour <strong>%s %s</strong>,</p>
                  <p>%s</p>
                  %s
                  <p style="color:#64748b;margin-top:20px">Cordialement,<br><strong>L'équipe RH — OCP</strong></p>
                </div></div></body></html>
                """.formatted(couleur, c.getPrenom(), c.getNom(), message,
                    c.getCommentaireRh() != null ? "<p><b>Commentaire RH:</b> " + c.getCommentaireRh() + "</p>" : "");
            emailService.envoyerEmail(c.getEmail(), sujet, contenu);
        } catch (Exception e) {
            log.warn("Email stagiaire non envoyé : {}", e.getMessage());
        }
    }

    private void envoyerEmailAcceptation(String email, String nom, String username, String password) {
        try {
            String contenu = """
                <html><body style="font-family:Arial,sans-serif">
                <div style="max-width:600px;margin:auto;padding:20px;border:1px solid #e2e8f0;border-radius:8px">
                <div style="background:#00843D;padding:16px;border-radius:6px 6px 0 0;text-align:center">
                  <h2 style="color:white;margin:0">OCP — Vos identifiants</h2>
                </div>
                <div style="padding:20px">
                  <p>Bonjour <strong>%s</strong>,</p>
                  <p>Voici vos identifiants pour accéder à la plateforme :</p>
                  <div style="background:#f0fdf4;border:1px solid #86efac;border-radius:8px;padding:16px;margin:16px 0">
                    <p><b>🔗 Plateforme :</b> http://localhost:4200</p>
                    <p><b>👤 Nom d'utilisateur :</b> %s</p>
                    <p><b>🔑 Mot de passe :</b> %s</p>
                  </div>
                  <p style="color:#dc2626">⚠️ Changez votre mot de passe après la première connexion.</p>
                </div></div></body></html>
                """.formatted(nom, username, password);
            emailService.envoyerEmail(email, "🎉 Bienvenue chez OCP — Vos identifiants", contenu);
        } catch (Exception e) {
            log.warn("Email credentials non envoyé : {}", e.getMessage());
        }
    }

    @Override
    public List<CandidatureResponse> findAll() {
        return candidatureRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<CandidatureResponse> findByStatut(String statut) {
        return candidatureRepository.findByStatutOrderByCreatedAtDesc(statut)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public CandidatureResponse findById(Long id) {
        return toResponse(candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable")));
    }

    @Override
    public byte[] getCv(Long id) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));
        return c.getCvContenu();
    }

    private CandidatureResponse toResponse(Candidature c) {
        CandidatureResponse r = new CandidatureResponse();
        r.setId(c.getId());
        r.setNom(c.getNom());
        r.setPrenom(c.getPrenom());
        r.setEmail(c.getEmail());
        r.setTelephone(c.getTelephone());
        r.setFiliere(c.getFiliere());
        r.setNiveau(c.getNiveau());
        r.setEtablissement(c.getEtablissement());
        r.setSujetSouhaite(c.getSujetSouhaite());
        r.setDepartementSouhaite(c.getDepartementSouhaite());
        r.setMessage(c.getMessage());
        r.setStatut(c.getStatut());
        r.setCommentaireRh(c.getCommentaireRh());
        r.setCvNomFichier(c.getCvNomFichier());
        r.setHasCv(c.getCvContenu() != null);
        r.setCreatedAt(c.getCreatedAt());
        r.setTraiteAt(c.getTraiteAt());
        r.setTraitePar(c.getTraitePar());
        r.setScoreMatching(c.getScoreMatching());
        r.setAnnonceId(c.getAnnonceId());
        return r;
    }
}