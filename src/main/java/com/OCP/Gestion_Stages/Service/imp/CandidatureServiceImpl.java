package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.EmailService;
import com.OCP.Gestion_Stages.Service.OllamaService;
import com.OCP.Gestion_Stages.Service.interfaces.CandidatureService;
import com.OCP.Gestion_Stages.Service.interfaces.CandidatureServiceExtended;
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
import com.OCP.Gestion_Stages.domain.dto.candidature.CandidatureDTO;
import com.OCP.Gestion_Stages.domain.enums.UserRole;
import org.springframework.context.ApplicationContext;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CandidatureServiceImpl implements CandidatureService, CandidatureServiceExtended {

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
    private final org.springframework.context.ApplicationContext applicationContext;
    private final DocumentCandidatureRepository documentCandidatureRepository;

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
        c.setSpecialite(request.getSpecialite());
        if (cv != null && !cv.isEmpty() && request.getSpecialite() != null) {
            try {
                org.apache.pdfbox.pdmodel.PDDocument pdfDoc =
                        org.apache.pdfbox.Loader.loadPDF(cv.getBytes());
                org.apache.pdfbox.text.PDFTextStripper stripper =
                        new org.apache.pdfbox.text.PDFTextStripper();
                String texteCV = stripper.getText(pdfDoc);
                pdfDoc.close();
                int score = ollamaService.calculerScoreMatchingSpecialite(
                        texteCV,
                        request.getSpecialite(),
                        request.getDepartementSouhaite() != null ? request.getDepartementSouhaite() : "OCP Group"
                );
                c.setScoreMatching(score);
                log.info("Score IA calculé: {} pour spécialité: {}", score, request.getSpecialite());
            } catch (Exception e) {
                log.warn("Erreur score IA: {}", e.getMessage());
            }
        }
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

        // Créer compte user
        User user = User.builder()
                .username(username).email(c.getEmail())
                .password(passwordEncoder.encode(password))
                .role(UserRole.STAGIAIRE).actif(true).build();
        userRepository.save(user);

        // Créer stagiaire
        Stagiaire stagiaire = new Stagiaire();
        stagiaire.setNom(c.getNom());
        stagiaire.setPrenom(c.getPrenom());
        stagiaire.setEmail(c.getEmail());
        stagiaire.setTelephone(c.getTelephone());
        stagiaire.setFiliere(c.getFiliere());
        stagiaire.setNiveau(c.getNiveau());
        stagiaire.setUser(user);

        // Affecter département
        if (request.getDepartementId() != null)
            departementRepository.findById(request.getDepartementId())
                    .ifPresent(stagiaire::setDepartement);

        // Chercher ou créer établissement
        if (c.getEtablissement() != null && !c.getEtablissement().isEmpty()) {
            com.OCP.Gestion_Stages.Repository.EtablissementRepository etabRepo =
                    applicationContext.getBean(com.OCP.Gestion_Stages.Repository.EtablissementRepository.class);
            etabRepo.findByNomContainingIgnoreCase(c.getEtablissement()).stream().findFirst()
                    .ifPresent(stagiaire::setEtablissement);
        }

        stagiaireRepository.save(stagiaire);

        // Créer stage
        Stage stage = new Stage();
        stage.setStagiaire(stagiaire);

        // Sujet : priorité au sujet fourni par RH, sinon sujet souhaité par candidat
        String sujet = (request.getSujet() != null && !request.getSujet().isEmpty())
                ? request.getSujet()
                : (c.getSujetSouhaite() != null ? c.getSujetSouhaite() : "Stage");
        stage.setSujet(sujet);

        // Type de stage
        if (request.getTypeStage() != null) {
            try {
                stage.setTypeStage(TypeStage.valueOf(request.getTypeStage()));
            } catch (Exception e) {
                stage.setTypeStage(TypeStage.PFE);
            }
        } else if (c.getAnnonceId() != null) {
            annonceRepository.findById(c.getAnnonceId())
                    .ifPresent(a -> {
                        try {
                            stage.setTypeStage(TypeStage.valueOf(a.getTypeStage()));
                        } catch (Exception ex) {
                            stage.setTypeStage(TypeStage.PFE);
                        }
                    });
        } else {
            stage.setTypeStage(TypeStage.PFE);
        }

        // Dates
        if (request.getDateDebut() != null) stage.setDateDebut(request.getDateDebut());
        if (request.getDateFin() != null) stage.setDateFin(request.getDateFin());

        stage.setStatut(StageStatus.VALIDEE);

        // Encadrant
        if (request.getEncadrantId() != null)
            encadrantRepository.findById(request.getEncadrantId()).ifPresent(stage::setEncadrant);

        // Département
        if (request.getDepartementId() != null)
            departementRepository.findById(request.getDepartementId()).ifPresent(stage::setDepartement);

        stageRepository.save(stage);

        // Créer checklist onboarding automatiquement
        creerChecklistOnboarding(stagiaire);

        // Email credentials
        envoyerEmailAcceptation(c.getEmail(), c.getPrenom() + " " + c.getNom(), username, password);
    }

    private void creerChecklistOnboarding(Stagiaire stagiaire) {
        try {
            com.OCP.Gestion_Stages.Repository.OnboardingChecklistRepository checklistRepo =
                    applicationContext.getBean(com.OCP.Gestion_Stages.Repository.OnboardingChecklistRepository.class);

            Object[][] etapes = {
                    {"Remise badge accès", "ADMINISTRATIF", "Remettre le badge d'accès OCP au stagiaire", 1},
                    {"Signature contrat stage", "ADMINISTRATIF", "Faire signer la convention de stage", 2},
                    {"Création compte informatique", "INFORMATIQUE", "Créer le compte Active Directory OCP", 3},
                    {"Accès messagerie OCP", "INFORMATIQUE", "Configurer la messagerie professionnelle", 4},
                    {"Visite des locaux", "INTEGRATION", "Faire visiter les locaux et présenter les équipes", 5},
                    {"Présentation au département", "INTEGRATION", "Présenter le stagiaire à l'équipe", 6},
                    {"Remise du matériel", "MATERIEL", "Remettre PC, téléphone ou matériel nécessaire", 7},
                    {"Formation sécurité", "FORMATION", "Formation aux règles de sécurité OCP", 8},
                    {"Présentation du projet", "FORMATION", "Présenter le sujet et les objectifs du stage", 9},
                    {"Accès outils métier", "INFORMATIQUE", "Configurer les accès aux outils spécifiques", 10}
            };

            for (Object[] etape : etapes) {
                com.OCP.Gestion_Stages.domain.model.OnboardingChecklist item =
                        new com.OCP.Gestion_Stages.domain.model.OnboardingChecklist();
                item.setStagiaire(stagiaire);
                item.setEtape((String) etape[0]);
                item.setCategorie((String) etape[1]);
                item.setDescription((String) etape[2]);
                item.setOrdre((Integer) etape[3]);
                item.setCompleted(false);
                checklistRepo.save(item);
            }
        } catch (Exception e) {
            log.warn("Checklist onboarding non créée : {}", e.getMessage());
        }
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
    @Transactional(readOnly = true)
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
        if (c.getDepartement() != null) {
            r.setDepartementNom(c.getDepartement().getNom());
            r.setDepartementId(c.getDepartement().getId());
        }
        return r;
    }
    

    @Override
    public CandidatureDTO planifierMeeting(Long id, String dateMeeting) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));
        c.setStatut("MEETING_PLANIFIE");
        c.setStatutMeeting("PLANIFIE");
        c.setDateMeeting(LocalDateTime.parse(dateMeeting));
        candidatureRepository.save(c);
        try {
            emailService.envoyerEmail(c.getEmail(),
                    "Meeting planifié — OCP Group",
                    "Bonjour " + c.getPrenom() + ",\n\nUn entretien a été planifié le " +
                            c.getDateMeeting() + ".\n\nCordialement,\nOCP Group");
        } catch (Exception ignored) {}
        return toDTO(c);
    }

    @Override
    public CandidatureDTO decisionEncadrant(Long id, String decision,
                                            String note, String username) throws Exception {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));
        c.setNoteEncadrant(note);
        c.setTraitePar(username);
        c.setTraiteAt(LocalDateTime.now());

        if ("ACCEPTE".equals(decision)) {
            c.setStatut("ACCEPTEE_ENCADRANT");
            c.setStatutMeeting("VALIDE");

            String usernameCandidat = (c.getPrenom().toLowerCase() + "." + c.getNom().toLowerCase())
                    .replaceAll("[^a-z.]", "");
            String password = "OCP@" + c.getId() + "2026";

            if (!userRepository.existsByUsername(usernameCandidat)) {
                User user = new User();
                user.setUsername(usernameCandidat);
                user.setEmail(c.getEmail());
                user.setPassword(passwordEncoder.encode(password));
                user.setRole(UserRole.STAGIAIRE);
                user.setActif(true);
                User savedUser = userRepository.save(user);
                c.setUsername(usernameCandidat);
                c.setPasswordTemp(password);

                // Créer Stagiaire
                boolean stagiaireExiste = stagiaireRepository.findAll().stream()
                        .anyMatch(s -> s.getEmail().equals(c.getEmail()));
                if (!stagiaireExiste) {
                    Stagiaire stagiaire = new Stagiaire();
                    stagiaire.setNom(c.getNom());
                    stagiaire.setPrenom(c.getPrenom());
                    stagiaire.setEmail(c.getEmail());
                    stagiaire.setTelephone(c.getTelephone());
                    stagiaire.setFiliere(c.getFiliere());
                    stagiaire.setNiveau(c.getNiveau());
                    stagiaire.setUser(savedUser);
                    if (c.getDepartement() != null)
                        stagiaire.setDepartement(c.getDepartement());
                    stagiaireRepository.save(stagiaire);
                }
            }

            // Créer Stage via traiter()
            try {
                TraiterCandidatureRequest req = new TraiterCandidatureRequest();
                req.setStatut("ACCEPTEE");
                req.setSujet(c.getSujetSouhaite() != null ? c.getSujetSouhaite() : "Stage OCP");
                req.setTypeStage("PFE");
                if (c.getDepartement() != null)
                    req.setDepartementId(c.getDepartement().getId());
                traiter(c.getId(), req, username);
            } catch (Exception ignored) {}

            try {
                emailService.envoyerEmail(c.getEmail(),
                        "✅ Candidature acceptée — OCP Group",
                        "Bonjour " + c.getPrenom() + ",\n\nFélicitations !\n" +
                                "Identifiants : " + c.getUsername() + " / " + c.getPasswordTemp() + "\n\n" +
                                "Documents à uploader :\n1. Convention établissement\n2. Assurance\n3. CIN\n4. CV\n\n" +
                                "Connectez-vous : http://localhost:4200\n\nOCP Group");
            } catch (Exception ignored) {}

        } else {
            c.setStatut("REFUSEE_ENCADRANT");
            c.setStatutMeeting("REFUSE");
            try {
                emailService.envoyerEmail(c.getEmail(),
                        "Résultat candidature — OCP Group",
                        "Bonjour " + c.getPrenom() + ",\nNous ne pouvons donner suite à votre candidature.\n" +
                                (note != null ? "Motif : " + note : "") + "\nOCP Group");
            } catch (Exception ignored) {}
        }

        candidatureRepository.save(c);
        return toDTO(c);
    }

    @Override
    public java.util.Map<String, Object> verifierIa(Long id) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));
        c.setStatut("VERIFICATION_IA");

        java.util.List<DocumentCandidature> docs = documentCandidatureRepository.findByCandidatureId(id);
        int scoreGlobal = 0;
        java.util.List<String> commentaires = new java.util.ArrayList<>();

        for (DocumentCandidature doc : docs) {
            int score = 75;
            String commentaire = "Document " + doc.getTypeDocument() + " : ";
            if (doc.getContenu() != null && doc.getContenu().length > 0) { score += 10; commentaire += "Fichier reçu ✓. "; }
            if (doc.getNomFichier() != null && doc.getNomFichier().toLowerCase().contains("pdf")) { score += 5; commentaire += "Format PDF ✓. "; }
            doc.setScoreIa(score);
            doc.setStatutIa(score >= 80 ? "VALIDE" : "SUSPECT");
            doc.setCommentaireIa(commentaire);
            documentCandidatureRepository.save(doc);
            scoreGlobal += score;
            commentaires.add(commentaire);
        }

        int scoreMoyen = docs.isEmpty() ? 0 : scoreGlobal / docs.size();
        c.setScoreMatching(scoreMoyen);
        candidatureRepository.save(c);

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("scoreMoyen", scoreMoyen);
        result.put("nbDocuments", docs.size());
        result.put("commentaires", commentaires);
        result.put("statut", scoreMoyen >= 80 ? "DOCUMENTS_VALIDES" : "DOCUMENTS_SUSPECTS");
        return result;
    }

    @Override
    public CandidatureDTO validerFinal(Long id, String decision,
                                       String commentaire, String username) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));
        c.setTraitePar(username);
        c.setTraiteAt(LocalDateTime.now());
        c.setCommentaireRh(commentaire);

        if ("VALIDE".equals(decision)) {
            c.setStatut("ACCEPTEE_RH");
            c.setConvocationEnvoyee(true);
            c.setDateConvocation(LocalDateTime.now());
            try {
                emailService.envoyerEmail(c.getEmail(),
                        "🎉 Convocation — OCP Group",
                        "Bonjour " + c.getPrenom() + ",\nVotre dossier a été validé.\n" +
                                "Connectez-vous : http://localhost:4200\nOCP Group");
            } catch (Exception ignored) {}
        } else {
            c.setStatut("REFUSEE_RH");
        }

        candidatureRepository.save(c);
        return toDTO(c);
    }

    @Override
    public List<CandidatureDTO> getCandidaturesDepartement(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User introuvable"));
        Encadrant encadrant = encadrantRepository.findAll().stream()
                .filter(e -> e.getUser() != null && e.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Encadrant introuvable"));
        if (encadrant.getDepartement() == null) return List.of();
        return candidatureRepository
                .findByDepartementIdOrderByCreatedAtDesc(encadrant.getDepartement().getId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public CandidatureDTO toDTO(Candidature c) {
        return CandidatureDTO.builder()
                .id(c.getId())
                .nom(c.getNom())
                .prenom(c.getPrenom())
                .email(c.getEmail())
                .telephone(c.getTelephone())
                .filiere(c.getFiliere())
                .niveau(c.getNiveau())
                .etablissement(c.getEtablissement())
                .specialite(c.getSpecialite())
                .sujetSouhaite(c.getSujetSouhaite())
                .message(c.getMessage())
                .statut(c.getStatut())
                .statutMeeting(c.getStatutMeeting() != null ? c.getStatutMeeting() : "SANS_MEETING")
                .dateMeeting(c.getDateMeeting())
                .noteEncadrant(c.getNoteEncadrant())
                .scoreMatching(c.getScoreMatching() != null ? c.getScoreMatching() : 0)
                .departementId(c.getDepartement() != null ? c.getDepartement().getId() : null)
                .departementNom(c.getDepartement() != null ? c.getDepartement().getNom() : "")
                .username(c.getUsername() != null ? c.getUsername() : "")
                .convocationEnvoyee(c.getConvocationEnvoyee() != null ? c.getConvocationEnvoyee() : false)
                .dateConvocation(c.getDateConvocation())
                .nbDocuments(documentCandidatureRepository.countByCandidatureId(c.getId()))
                .createdAt(c.getCreatedAt())
                .traiteAt(c.getTraiteAt())
                .traitePar(c.getTraitePar())
                .build();
    }


}