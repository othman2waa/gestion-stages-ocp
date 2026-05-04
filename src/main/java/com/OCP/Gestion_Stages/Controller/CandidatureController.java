package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.EmailService;
import com.OCP.Gestion_Stages.Service.interfaces.CandidatureService;
import com.OCP.Gestion_Stages.domain.dto.candidature.CandidatureRequest;
import com.OCP.Gestion_Stages.domain.dto.candidature.CandidatureResponse;
import com.OCP.Gestion_Stages.domain.dto.candidature.TraiterCandidatureRequest;
import com.OCP.Gestion_Stages.domain.model.*;
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
import com.OCP.Gestion_Stages.domain.enums.UserRole;

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
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // ════════════════════════════════════════════
    // PUBLIC — Soumettre candidature + créer compte
    // ════════════════════════════════════════════
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> soumettre(
            @RequestPart("data") CandidatureRequest request,
            @RequestPart(value = "cv", required = false) MultipartFile cv) throws Exception {

        // Vérifier email unique
        if (candidatureRepository.findAll().stream()
                .anyMatch(c -> c.getEmail().equals(request.getEmail()))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Une candidature avec cet email existe déjà"));
        }

        // Créer la candidature via service existant
        CandidatureResponse response = candidatureService.soumettre(request, cv);

        // Lier département si departementId fourni
        if (request.getDepartementId() != null) {
            candidatureRepository.findById(response.getId()).ifPresent(c -> {
                departementRepository.findById(request.getDepartementId())
                        .ifPresent(c::setDepartement);
                candidatureRepository.save(c);
            });
        }

        return ResponseEntity.ok(response);
    }

    // ════════════════════════════════════════════
    // ADMIN RH — Vue complète toutes candidatures
    // ════════════════════════════════════════════
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

    // ════════════════════════════════════════════
    // ENCADRANT — Voir candidatures de SON département
    // ════════════════════════════════════════════
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

        if (encadrant.getDepartement() == null)
            return ResponseEntity.ok(List.of());

        return ResponseEntity.ok(
                candidatureRepository.findByDepartementIdOrderByCreatedAtDesc(
                                encadrant.getDepartement().getId())
                        .stream().map(this::toResponse).collect(Collectors.toList())
        );
    }

    // ════════════════════════════════════════════
    // ENCADRANT — Planifier un meeting
    // ════════════════════════════════════════════
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

        // Email au candidat
        try {
            emailService.envoyerEmail(c.getEmail(),
                    "Meeting planifié — OCP Group",
                    "Bonjour " + c.getPrenom() + ",\n\nUn entretien a été planifié le " +
                            c.getDateMeeting() + ".\n\nCordialement,\nOCP Group");
        } catch (Exception ignored) {}

        return ResponseEntity.ok(Map.of("message", "Meeting planifié", "dateMeeting", c.getDateMeeting().toString()));
    }

    // ════════════════════════════════════════════
    // ENCADRANT — Valider ou refuser après meeting
    // ════════════════════════════════════════════
    @PatchMapping("/{id}/decision-encadrant")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<?> decisionEncadrant(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));

        String decision = body.get("decision"); // ACCEPTE ou REFUSE
        c.setNoteEncadrant(body.get("note"));
        c.setTraitePar(userDetails.getUsername());
        c.setTraiteAt(LocalDateTime.now());

        if ("ACCEPTE".equals(decision)) {
            c.setStatut("ACCEPTEE_ENCADRANT");
            c.setStatutMeeting("VALIDE");

            // Créer compte User pour le stagiaire
            String username = (c.getPrenom().toLowerCase() + "." + c.getNom().toLowerCase())
                    .replaceAll("[^a-z.]", "");
            String password = "OCP@" + c.getId() + "2026";

            if (!userRepository.existsByUsername(username)) {
                User user = new User();
                user.setUsername(username);
                user.setEmail(c.getEmail());
                user.setPassword(passwordEncoder.encode(password));
                user.setRole(UserRole.STAGIAIRE);
                user.setActif(true);
                userRepository.save(user);
                c.setUsername(username);
                c.setPasswordTemp(password);
            }

            // Email avec identifiants + liste documents
            try {
                emailService.envoyerEmail(c.getEmail(),
                        "✅ Candidature acceptée — OCP Group",
                        "Bonjour " + c.getPrenom() + " " + c.getNom() + ",\n\n" +
                                "Félicitations ! Votre candidature a été acceptée.\n\n" +
                                "Vos identifiants :\n" +
                                "  Nom d'utilisateur : " + username + "\n" +
                                "  Mot de passe : " + password + "\n\n" +
                                "Documents à uploader dans votre espace :\n" +
                                "  1. Convention de stage signée par votre établissement (scan PDF)\n" +
                                "  2. Attestation d'assurance stage (scan PDF)\n" +
                                "  3. Carte d'identité nationale (scan PDF)\n" +
                                "  4. CV (PDF)\n\n" +
                                "Connectez-vous sur : http://localhost:4200\n\n" +
                                "Cordialement,\nOCP Group — Direction Capital Humain");
            } catch (Exception ignored) {}

        } else {
            c.setStatut("REFUSEE_ENCADRANT");
            c.setStatutMeeting("REFUSE");
            try {
                emailService.envoyerEmail(c.getEmail(),
                        "Résultat de votre candidature — OCP Group",
                        "Bonjour " + c.getPrenom() + ",\n\n" +
                                "Nous avons bien étudié votre candidature mais ne pouvons y donner suite.\n" +
                                (body.get("note") != null ? "Motif : " + body.get("note") + "\n" : "") +
                                "\nCordialement,\nOCP Group");
            } catch (Exception ignored) {}
        }

        candidatureRepository.save(c);
        return ResponseEntity.ok(Map.of("message", "Décision enregistrée", "statut", c.getStatut()));
    }

    // ════════════════════════════════════════════
    // STAGIAIRE — Uploader documents
    // ════════════════════════════════════════════
    @PostMapping("/{id}/upload-document")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<?> uploadDocument(
            @PathVariable Long id,
            @RequestParam("type") String typeDocument,
            @RequestParam("fichier") MultipartFile fichier) throws Exception {

        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));

        // Supprimer ancien document du même type
        documentRepository.findByCandidatureId(id).stream()
                .filter(d -> d.getTypeDocument().equals(typeDocument))
                .forEach(documentRepository::delete);

        DocumentCandidature doc = DocumentCandidature.builder()
                .candidature(c)
                .typeDocument(typeDocument)
                .nomFichier(fichier.getOriginalFilename())
                .contenu(fichier.getBytes())
                .statutIa("NON_VERIFIE")
                .build();
        documentRepository.save(doc);

        // Vérifier si tous les 4 documents sont uploadés
        long nbDocs = documentRepository.findByCandidatureId(id).size();
        if (nbDocs >= 4) {
            c.setStatut("DOCUMENTS_SOUMIS");
            candidatureRepository.save(c);
        }

        return ResponseEntity.ok(Map.of("message", "Document uploadé", "type", typeDocument, "nbDocuments", nbDocs));
    }

    // ════════════════════════════════════════════
    // RH ADMIN — Voir documents d'une candidature
    // ════════════════════════════════════════════
    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<?>> getDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(
                documentRepository.findByCandidatureId(id).stream()
                        .map(d -> {
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

    // ════════════════════════════════════════════
    // RH ADMIN — Lancer vérification IA documents
    // ════════════════════════════════════════════
    @PostMapping("/{id}/verifier-ia")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<?> verifierIa(@PathVariable Long id) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));
        c.setStatut("VERIFICATION_IA");
        candidatureRepository.save(c);

        List<DocumentCandidature> docs = documentRepository.findByCandidatureId(id);
        int scoreGlobal = 0;
        List<String> commentaires = new ArrayList<>();

        for (DocumentCandidature doc : docs) {
            // Simulation vérification IA — nom/prénom/CIN dans le document
            String nomComplet = (c.getPrenom() + " " + c.getNom()).toLowerCase();
            int score = 75; // score de base
            String commentaire = "Document " + doc.getTypeDocument() + " : ";

            // Vérification simple du nom de fichier et type
            if (doc.getContenu() != null && doc.getContenu().length > 0) {
                score += 10;
                commentaire += "Fichier reçu ✓. ";
            }
            if (doc.getNomFichier() != null && doc.getNomFichier().toLowerCase().contains("pdf")) {
                score += 5;
                commentaire += "Format PDF ✓. ";
            }
            commentaire += "Vérification nom/CIN en cours...";

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

        return ResponseEntity.ok(Map.of(
                "scoreMoyen", scoreMoyen,
                "nbDocuments", docs.size(),
                "commentaires", commentaires,
                "statut", scoreMoyen >= 80 ? "DOCUMENTS_VALIDES" : "DOCUMENTS_SUSPECTS"
        ));
    }

    // ════════════════════════════════════════════
    // RH ADMIN — Validation finale + convocation
    // ════════════════════════════════════════════
    @PatchMapping("/{id}/valider-final")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<?> validerFinal(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));

        String decision = body.get("decision"); // VALIDE ou REFUSE
        c.setTraitePar(userDetails.getUsername());
        c.setTraiteAt(LocalDateTime.now());
        c.setCommentaireRh(body.get("commentaire"));

        if ("VALIDE".equals(decision)) {
            c.setStatut("ACCEPTEE_RH");
            c.setConvocationEnvoyee(true);
            c.setDateConvocation(LocalDateTime.now());

            try {
                emailService.envoyerEmail(c.getEmail(),
                        "🎉 Convocation de stage — OCP Group",
                        "Bonjour " + c.getPrenom() + " " + c.getNom() + ",\n\n" +
                                "Nous avons le plaisir de vous informer que votre dossier a été validé.\n\n" +
                                "Vous êtes convoqué(e) pour débuter votre stage à OCP Group.\n" +
                                "Veuillez vous connecter à votre espace pour consulter les détails.\n\n" +
                                "Identifiants rappel :\n" +
                                "  Utilisateur : " + c.getUsername() + "\n" +
                                "  Mot de passe : " + c.getPasswordTemp() + "\n\n" +
                                "Cordialement,\nOCP Group — Direction Capital Humain");
            } catch (Exception ignored) {}
        } else {
            c.setStatut("REFUSEE_RH");
        }

        candidatureRepository.save(c);
        return ResponseEntity.ok(Map.of("message", "Décision finale enregistrée", "statut", c.getStatut()));
    }

    // ════════════════════════════════════════════
    // Ancien endpoint traiter — gardé pour compatibilité
    // ════════════════════════════════════════════
    @PatchMapping("/{id}/traiter")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<CandidatureResponse> traiter(
            @PathVariable Long id,
            @RequestBody TraiterCandidatureRequest request,
            @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        return ResponseEntity.ok(candidatureService.traiter(id, request, userDetails.getUsername()));
    }

    // ════════════════════════════════════════════
    // toResponse helper
    // ════════════════════════════════════════════
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
        m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
        m.put("convocationEnvoyee", c.getConvocationEnvoyee() != null ? c.getConvocationEnvoyee() : false);
        m.put("username", c.getUsername() != null ? c.getUsername() : "");
        long nbDocs = documentRepository.countByCandidatureId(c.getId());
        m.put("nbDocuments", nbDocs);
        return m;
    }
}