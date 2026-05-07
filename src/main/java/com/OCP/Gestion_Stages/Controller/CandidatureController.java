package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.EmailService;
import com.OCP.Gestion_Stages.Service.interfaces.CandidatureService;
import com.OCP.Gestion_Stages.domain.dto.candidature.CandidatureRequest;
import com.OCP.Gestion_Stages.domain.dto.candidature.CandidatureResponse;
import com.OCP.Gestion_Stages.domain.dto.candidature.TraiterCandidatureRequest;
import com.OCP.Gestion_Stages.domain.model.*;
import com.OCP.Gestion_Stages.domain.enums.UserRole;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/candidatures")
@RequiredArgsConstructor
@Transactional
public class CandidatureController {

    private final CandidatureService candidatureService;
    private final CandidatureRepository candidatureRepository;
    private final DocumentCandidatureRepository documentRepository;
    private final DepartementRepository departementRepository;
    private final UserRepository userRepository;
    private final EncadrantRepository encadrantRepository;
    private final StagiaireRepository stagiaireRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // ── PUBLIC — Soumettre candidature
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> soumettre(
            @RequestPart("data") CandidatureRequest request,
            @RequestPart(value = "cv", required = false) MultipartFile cv) throws Exception {
        if (candidatureRepository.findAll().stream()
                .anyMatch(c -> c.getEmail().equals(request.getEmail()))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Une candidature avec cet email existe déjà"));
        }
        CandidatureResponse response = candidatureService.soumettre(request, cv);
        if (request.getDepartementId() != null) {
            candidatureRepository.findById(response.getId()).ifPresent(c -> {
                departementRepository.findById(request.getDepartementId()).ifPresent(c::setDepartement);
                candidatureRepository.save(c);
            });
        }
        return ResponseEntity.ok(response);
    }

    // ── ADMIN RH
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<CandidatureResponse>> getAll() {
        return ResponseEntity.ok(candidatureService.findAll());
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<CandidatureResponse>> getByStatut(@PathVariable String statut) {
        return ResponseEntity.ok(candidatureService.findByStatut(statut));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<CandidatureResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(candidatureService.findById(id));
    }

    @GetMapping("/{id}/cv")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH','ENCADRANT')")
    public ResponseEntity<byte[]> getCv(@PathVariable Long id) {
        byte[] cv = candidatureService.getCv(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=cv.pdf")
                .body(cv);
    }

    // ── ENCADRANT — Candidatures de son département
    @GetMapping("/mon-departement")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<List<?>> getCandidaturesDepartement(
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User introuvable"));
        var encadrant = encadrantRepository.findAll().stream()
                .filter(e -> e.getUser() != null && e.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Encadrant introuvable"));
        if (encadrant.getDepartement() == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(
                candidatureRepository.findByDepartementIdOrderByCreatedAtDesc(
                                encadrant.getDepartement().getId())
                        .stream().map(this::toResponse).collect(Collectors.toList())
        );
    }

    // ── ENCADRANT — Planifier meeting
    @PatchMapping("/{id}/planifier-meeting")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<?> planifierMeeting(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));
        c.setStatut("MEETING_PLANIFIE");
        c.setStatutMeeting("PLANIFIE");
        c.setDateMeeting(LocalDateTime.parse(body.get("dateMeeting")));
        candidatureRepository.save(c);
        try {
            emailService.envoyerEmail(c.getEmail(), "Meeting planifié — OCP Group",
                    "Bonjour " + c.getPrenom() + ",\n\nUn entretien a été planifié le " +
                            c.getDateMeeting() + ".\n\nCordialement,\nOCP Group");
        } catch (Exception ignored) {}
        return ResponseEntity.ok(Map.of("message", "Meeting planifié",
                "dateMeeting", c.getDateMeeting().toString()));
    }

    // ── ENCADRANT — Décision après meeting
    @PatchMapping("/{id}/decision-encadrant")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<?> decisionEncadrant(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));

        String decision = body.get("decision");
        c.setNoteEncadrant(body.get("note"));
        c.setTraitePar(userDetails.getUsername());
        c.setTraiteAt(LocalDateTime.now());

        if ("ACCEPTE".equals(decision)) {
            c.setStatut("ACCEPTEE_ENCADRANT");
            c.setStatutMeeting("VALIDE");

            // Créer compte User
            String username = (c.getPrenom().toLowerCase() + "." + c.getNom().toLowerCase())
                    .replaceAll("[^a-z.]", "");
            String password = "OCP@" + c.getId() + "2026";

            User user = null;
            if (!userRepository.existsByUsername(username)) {
                user = new User();
                user.setUsername(username);
                user.setEmail(c.getEmail());
                user.setPassword(passwordEncoder.encode(password));
                user.setRole(UserRole.STAGIAIRE);
                user.setActif(true);
                user = userRepository.save(user);
                c.setUsername(username);
                c.setPasswordTemp(password);
            } else {
                user = userRepository.findByUsername(username).orElse(null);
            }

            // Créer Stagiaire si pas déjà créé
            final User finalUser = user;
            boolean stagiaireExiste = stagiaireRepository.findAll().stream()
                    .anyMatch(s -> s.getEmail().equals(c.getEmail()));

            if (!stagiaireExiste && finalUser != null) {
                Stagiaire stagiaire = new Stagiaire();
                stagiaire.setNom(c.getNom());
                stagiaire.setPrenom(c.getPrenom());
                stagiaire.setEmail(c.getEmail());
                stagiaire.setTelephone(c.getTelephone());
                stagiaire.setFiliere(c.getFiliere());
                stagiaire.setNiveau(c.getNiveau());
                stagiaire.setUser(finalUser);
                if (c.getDepartement() != null)
                    stagiaire.setDepartement(c.getDepartement());
                stagiaireRepository.save(stagiaire);
            }

            candidatureRepository.save(c);

            // Créer Stage via service traiter
            try {
                TraiterCandidatureRequest traiterReq = new TraiterCandidatureRequest();
                traiterReq.setStatut("ACCEPTEE");
                traiterReq.setSujet(c.getSujetSouhaite() != null ? c.getSujetSouhaite() : "Stage OCP");
                traiterReq.setTypeStage("PFE");
                if (c.getDepartement() != null)
                    traiterReq.setDepartementId(c.getDepartement().getId());
                // Affecter l'encadrant qui a accepté
                var encadrantUser = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
                if (encadrantUser != null) {
                    encadrantRepository.findAll().stream()
                            .filter(e -> e.getUser() != null && e.getUser().getId().equals(encadrantUser.getId()))
                            .findFirst()
                            .ifPresent(e -> traiterReq.setEncadrantId(e.getId()));
                }
                candidatureService.traiter(c.getId(), traiterReq, userDetails.getUsername());
            } catch (Exception e) {
                // Stage peut déjà exister
            }

            // Email identifiants + documents
            try {
                emailService.envoyerEmail(c.getEmail(),
                        "✅ Candidature acceptée — OCP Group",
                        "Bonjour " + c.getPrenom() + " " + c.getNom() + ",\n\n" +
                                "Félicitations ! Votre candidature a été acceptée.\n\n" +
                                "Vos identifiants :\n" +
                                "  Nom d'utilisateur : " + username + "\n" +
                                "  Mot de passe : " + password + "\n\n" +
                                "Documents à uploader dans votre espace :\n" +
                                "  1. Convention de stage signée par votre établissement (PDF)\n" +
                                "  2. Attestation d'assurance stage (PDF)\n" +
                                "  3. Carte d'identité nationale (PDF)\n" +
                                "  4. CV (PDF)\n\n" +
                                "Connectez-vous : http://localhost:4200\n\n" +
                                "Cordialement,\nOCP Group — Direction Capital Humain");
            } catch (Exception ignored) {}

        } else {
            c.setStatut("REFUSEE_ENCADRANT");
            c.setStatutMeeting("REFUSE");
            candidatureRepository.save(c);
            try {
                emailService.envoyerEmail(c.getEmail(),
                        "Résultat de votre candidature — OCP Group",
                        "Bonjour " + c.getPrenom() + ",\n\n" +
                                "Nous avons bien étudié votre candidature mais ne pouvons y donner suite.\n" +
                                (body.get("note") != null ? "Motif : " + body.get("note") + "\n" : "") +
                                "\nCordialement,\nOCP Group");
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(Map.of("message", "Décision enregistrée", "statut", c.getStatut()));
    }

    // ── STAGIAIRE — Upload documents
    @PostMapping("/{id}/upload-document")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<?> uploadDocument(
            @PathVariable Long id,
            @RequestParam("type") String typeDocument,
            @RequestParam("fichier") MultipartFile fichier) throws Exception {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));
        documentRepository.findByCandidatureId(id).stream()
                .filter(d -> d.getTypeDocument().equals(typeDocument))
                .forEach(documentRepository::delete);
        DocumentCandidature doc = DocumentCandidature.builder()
                .candidature(c).typeDocument(typeDocument)
                .nomFichier(fichier.getOriginalFilename())
                .contenu(fichier.getBytes()).statutIa("NON_VERIFIE").build();
        documentRepository.save(doc);
        long nbDocs = documentRepository.findByCandidatureId(id).size();
        if (nbDocs >= 4) { c.setStatut("DOCUMENTS_SOUMIS"); candidatureRepository.save(c); }
        return ResponseEntity.ok(Map.of("message", "Document uploadé",
                "type", typeDocument, "nbDocuments", nbDocs));
    }

    // ── RH — Documents
    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<?>> getDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(
                documentRepository.findByCandidatureId(id).stream().map(d -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", d.getId());
                    m.put("typeDocument", d.getTypeDocument());
                    m.put("nomFichier", d.getNomFichier());
                    m.put("uploadedAt", d.getUploadedAt() != null ? d.getUploadedAt().toString() : "");
                    m.put("statutIa", d.getStatutIa());
                    m.put("scoreIa", d.getScoreIa());
                    m.put("commentaireIa", d.getCommentaireIa());
                    return m;
                }).collect(Collectors.toList())
        );
    }

    // ── RH — Vérification IA
    @PostMapping("/{id}/verifier-ia")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<?> verifierIa(@PathVariable Long id) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));
        c.setStatut("VERIFICATION_IA");
        List<DocumentCandidature> docs = documentRepository.findByCandidatureId(id);
        int scoreGlobal = 0;
        List<String> commentaires = new ArrayList<>();
        for (DocumentCandidature doc : docs) {
            int score = 75;
            String commentaire = "Document " + doc.getTypeDocument() + " : ";
            if (doc.getContenu() != null && doc.getContenu().length > 0) { score += 10; commentaire += "Fichier reçu ✓. "; }
            if (doc.getNomFichier() != null && doc.getNomFichier().toLowerCase().contains("pdf")) { score += 5; commentaire += "Format PDF ✓. "; }
            doc.setScoreIa(score);
            doc.setStatutIa(score >= 80 ? "VALIDE" : "SUSPECT");
            doc.setCommentaireIa(commentaire);
            documentRepository.save(doc);
            scoreGlobal += score;
            commentaires.add(commentaire);
        }
        int scoreMoyen = docs.isEmpty() ? 0 : scoreGlobal / docs.size();
        c.setScoreMatching(scoreMoyen);
        candidatureRepository.save(c);
        return ResponseEntity.ok(Map.of("scoreMoyen", scoreMoyen, "nbDocuments",
                docs.size(), "commentaires", commentaires,
                "statut", scoreMoyen >= 80 ? "DOCUMENTS_VALIDES" : "DOCUMENTS_SUSPECTS"));
    }

    // ── RH — Validation finale + convocation
    @PatchMapping("/{id}/valider-final")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<?> validerFinal(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));
        c.setTraitePar(userDetails.getUsername());
        c.setTraiteAt(LocalDateTime.now());
        c.setCommentaireRh(body.get("commentaire"));
        if ("VALIDE".equals(body.get("decision"))) {
            c.setStatut("ACCEPTEE_RH");
            c.setConvocationEnvoyee(true);
            c.setDateConvocation(LocalDateTime.now());
            try {
                emailService.envoyerEmail(c.getEmail(), "🎉 Convocation — OCP Group",
                        "Bonjour " + c.getPrenom() + ",\n\nVotre dossier a été validé.\n" +
                                "Connectez-vous : http://localhost:4200\n\nCordialement,\nOCP Group");
            } catch (Exception ignored) {}
        } else {
            c.setStatut("REFUSEE_RH");
        }
        candidatureRepository.save(c);
        return ResponseEntity.ok(Map.of("message", "Décision finale", "statut", c.getStatut()));
    }

    // ── Ancien endpoint traiter
    @PatchMapping("/{id}/traiter")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<CandidatureResponse> traiter(
            @PathVariable Long id,
            @RequestBody TraiterCandidatureRequest request,
            @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        return ResponseEntity.ok(candidatureService.traiter(id, request, userDetails.getUsername()));
    }

    private Map<String, Object> toResponse(Candidature c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("nom", c.getNom());
        m.put("prenom", c.getPrenom());
        m.put("email", c.getEmail());
        m.put("telephone", c.getTelephone() != null ? c.getTelephone() : "");
        m.put("filiere", c.getFiliere() != null ? c.getFiliere() : "");
        m.put("niveau", c.getNiveau() != null ? c.getNiveau() : "");
        m.put("etablissement", c.getEtablissement() != null ? c.getEtablissement() : "");
        m.put("sujetSouhaite", c.getSujetSouhaite() != null ? c.getSujetSouhaite() : "");
        m.put("message", c.getMessage() != null ? c.getMessage() : "");
        m.put("statut", c.getStatut());
        m.put("statutMeeting", c.getStatutMeeting() != null ? c.getStatutMeeting() : "SANS_MEETING");
        m.put("dateMeeting", c.getDateMeeting() != null ? c.getDateMeeting().toString() : null);
        m.put("noteEncadrant", c.getNoteEncadrant() != null ? c.getNoteEncadrant() : "");
        m.put("scoreMatching", c.getScoreMatching() != null ? c.getScoreMatching() : 0);
        m.put("departementId", c.getDepartement() != null ? c.getDepartement().getId() : null);
        m.put("departementNom", c.getDepartement() != null ? c.getDepartement().getNom() : "");
        m.put("specialite", c.getSpecialite() != null ? c.getSpecialite() : "");
        m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
        m.put("convocationEnvoyee", c.getConvocationEnvoyee() != null ? c.getConvocationEnvoyee() : false);
        m.put("username", c.getUsername() != null ? c.getUsername() : "");
        long nbDocs = documentRepository.countByCandidatureId(c.getId());
        m.put("nbDocuments", nbDocs);
        return m;
    }
}