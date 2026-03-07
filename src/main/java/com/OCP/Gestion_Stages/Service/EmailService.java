package com.OCP.Gestion_Stages.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void envoyerEmail(String destinataire, String sujet, String contenuHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(contenuHtml, true);
            helper.setFrom("noreply@gestion-stages.com");
            mailSender.send(message);
            log.info("Email envoyé à {}", destinataire);
        } catch (Exception e) {
            log.error("Erreur envoi email à {}: {}", destinataire, e.getMessage());
        }
    }

    @Async
    public void envoyerBienvenue(String destinataire, String nomStagiaire) {
        String sujet = "Bienvenue dans le système de gestion des stages - OCP";
        String contenu = """
            <html><body style="font-family: Arial, sans-serif; color: #333;">
            <div style="max-width:600px;margin:auto;padding:20px;border:1px solid #e2e8f0;border-radius:8px;">
                <div style="background:#1e40af;padding:20px;border-radius:6px 6px 0 0;text-align:center;">
                    <h1 style="color:white;margin:0;">OCP — Gestion des Stages</h1>
                </div>
                <div style="padding:24px;">
                    <h2>Bienvenue, %s !</h2>
                    <p>Votre dossier de stage a été créé avec succès dans notre système.</p>
                    <p>Vous pouvez suivre l'avancement de votre stage via notre plateforme.</p>
                    <br>
                    <p style="color:#64748b;font-size:0.875rem;">Cordialement,<br><strong>L'équipe RH — OCP</strong></p>
                </div>
            </div>
            </body></html>
            """.formatted(nomStagiaire);
        envoyerEmail(destinataire, sujet, contenu);
    }

    @Async
    public void envoyerChangementStatut(String destinataire, String nomStagiaire,
                                        String sujetStage, String ancienStatut, String nouveauStatut,
                                        String commentaire) {
        String sujet = "Mise à jour du statut de votre stage — " + nouveauStatut;
        String contenu = """
            <html><body style="font-family: Arial, sans-serif; color: #333;">
            <div style="max-width:600px;margin:auto;padding:20px;border:1px solid #e2e8f0;border-radius:8px;">
                <div style="background:#1e40af;padding:20px;border-radius:6px 6px 0 0;text-align:center;">
                    <h1 style="color:white;margin:0;">OCP — Gestion des Stages</h1>
                </div>
                <div style="padding:24px;">
                    <h2>Mise à jour de votre stage</h2>
                    <p>Bonjour <strong>%s</strong>,</p>
                    <p>Le statut de votre stage <strong>"%s"</strong> a été mis à jour :</p>
                    <div style="background:#f8fafc;border-left:4px solid #3b82f6;padding:16px;margin:16px 0;border-radius:4px;">
                        <p style="margin:0;"><strong>Ancien statut :</strong> %s</p>
                        <p style="margin:8px 0 0;"><strong>Nouveau statut :</strong> <span style="color:#1e40af;font-weight:bold;">%s</span></p>
                        %s
                    </div>
                    <p style="color:#64748b;font-size:0.875rem;">Cordialement,<br><strong>L'équipe RH — OCP</strong></p>
                </div>
            </div>
            </body></html>
            """.formatted(
                nomStagiaire, sujetStage, ancienStatut, nouveauStatut,
                commentaire != null && !commentaire.isEmpty()
                        ? "<p style='margin:8px 0 0;'><strong>Commentaire :</strong> " + commentaire + "</p>"
                        : ""
        );
        envoyerEmail(destinataire, sujet, contenu);
    }

    @Async
    public void envoyerRappelFinStage(String destinataire, String nomStagiaire,
                                      String sujetStage, String dateFin) {
        String sujet = "Rappel — Fin de stage dans 7 jours";
        String contenu = """
            <html><body style="font-family: Arial, sans-serif; color: #333;">
            <div style="max-width:600px;margin:auto;padding:20px;border:1px solid #e2e8f0;border-radius:8px;">
                <div style="background:#f59e0b;padding:20px;border-radius:6px 6px 0 0;text-align:center;">
                    <h1 style="color:white;margin:0;">⚠️ Rappel de fin de stage</h1>
                </div>
                <div style="padding:24px;">
                    <p>Bonjour <strong>%s</strong>,</p>
                    <p>Votre stage <strong>"%s"</strong> se termine dans <strong>7 jours</strong> (le <strong>%s</strong>).</p>
                    <p>Pensez à :</p>
                    <ul>
                        <li>Préparer votre rapport de stage</li>
                        <li>Planifier votre évaluation finale</li>
                        <li>Contacter votre encadrant</li>
                    </ul>
                    <p style="color:#64748b;font-size:0.875rem;">Cordialement,<br><strong>L'équipe RH — OCP</strong></p>
                </div>
            </div>
            </body></html>
            """.formatted(nomStagiaire, sujetStage, dateFin);
        envoyerEmail(destinataire, sujet, contenu);
    }
}