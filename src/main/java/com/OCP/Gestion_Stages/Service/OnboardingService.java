package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.domain.enums.UserRole;
import com.OCP.Gestion_Stages.domain.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final OllamaService ollamaService;
    private final EmailService emailService;
    private final StagiaireRepository stagiaireRepository;
    private final UserRepository userRepository;
    private final EtablissementRepository etablissementRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> analyserCV(MultipartFile file) throws Exception {
        String texte = extraireTextePDF(file);
        String jsonStr = ollamaService.extraireInfosCV(texte);
        JsonNode infos = objectMapper.readTree(jsonStr);

        Map<String, Object> result = new HashMap<>();
        result.put("nom", getField(infos, "nom"));
        result.put("prenom", getField(infos, "prenom"));
        result.put("email", getField(infos, "email"));
        result.put("telephone", getField(infos, "telephone"));
        result.put("filiere", getField(infos, "filiere"));
        result.put("niveau", getField(infos, "niveau"));
        result.put("etablissement", getField(infos, "etablissement"));
        result.put("departementSuggere", getField(infos, "departementSuggere"));
        result.put("sujetStage", getField(infos, "sujetStage"));
        return result;
    }

    @Transactional
    public Map<String, Object> creerCompteDepuisCV(Map<String, Object> infos) {
        String nom = (String) infos.get("nom");
        String prenom = (String) infos.get("prenom");
        String email = (String) infos.get("email");

        // Générer username et password
        String username = (prenom.toLowerCase() + "." + nom.toLowerCase())
                .replaceAll("[^a-z.]", "").replaceAll("\\s+", "");
        String plainPassword = genererMotDePasse();

        // Créer User
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(plainPassword))
                .role(UserRole.STAGIAIRE)
                .actif(true)
                .build();
        userRepository.save(user);

        // Créer Stagiaire
        Stagiaire stagiaire = new Stagiaire();
        stagiaire.setNom(nom);
        stagiaire.setPrenom(prenom);
        stagiaire.setEmail(email);
        stagiaire.setTelephone((String) infos.getOrDefault("telephone", ""));
        stagiaire.setFiliere((String) infos.getOrDefault("filiere", ""));
        stagiaire.setNiveau((String) infos.getOrDefault("niveau", ""));
        stagiaire.setUser(user);
        stagiaireRepository.save(stagiaire);

        // Envoyer email avec credentials
        envoyerEmailCredentials(email, prenom + " " + nom, username, plainPassword);

        Map<String, Object> result = new HashMap<>();
        result.put("stagiaireId", stagiaire.getId());
        result.put("username", username);
        result.put("message", "Compte créé avec succès");
        return result;
    }

    private String extraireTextePDF(MultipartFile file) throws Exception {
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String getField(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull()
                ? node.get(field).asText() : "";
    }

    private String genererMotDePasse() {
        return "OCP@" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void envoyerEmailCredentials(String email, String nom, String username, String password) {
        String sujet = "Vos identifiants de connexion — OCP Gestion des Stages";
        String contenu = """
            <html><body style="font-family: Arial, sans-serif; color: #333;">
            <div style="max-width:600px;margin:auto;padding:20px;border:1px solid #e2e8f0;border-radius:8px;">
                <div style="background:#1e40af;padding:20px;border-radius:6px 6px 0 0;text-align:center;">
                    <h1 style="color:white;margin:0;">OCP — Gestion des Stages</h1>
                </div>
                <div style="padding:24px;">
                    <h2>Bienvenue, %s !</h2>
                    <p>Votre dossier a été traité. Voici vos identifiants de connexion :</p>
                    <div style="background:#f0f9ff;border:1px solid #bae6fd;border-radius:8px;padding:16px;margin:16px 0;">
                        <p style="margin:0;"><strong>🔗 Plateforme :</strong> http://localhost:4200</p>
                        <p style="margin:8px 0 0;"><strong>👤 Nom d'utilisateur :</strong> %s</p>
                        <p style="margin:8px 0 0;"><strong>🔑 Mot de passe :</strong> %s</p>
                    </div>
                    <p style="color:#dc2626;font-size:0.85rem;">⚠️ Changez votre mot de passe après la première connexion.</p>
                    <p style="color:#64748b;font-size:0.875rem;">Cordialement,<br><strong>L'équipe RH — OCP</strong></p>
                </div>
            </div>
            </body></html>
            """.formatted(nom, username, password);
        emailService.envoyerEmail(email, sujet, contenu);
    }
}