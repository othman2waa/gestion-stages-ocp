package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.NotificationRepository;
import com.OCP.Gestion_Stages.Repository.UserRepository;
import com.OCP.Gestion_Stages.Service.interfaces.NotificationService;
import com.OCP.Gestion_Stages.domain.model.Notification;
import com.OCP.Gestion_Stages.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public List<Notification> getMesNotifications(String username) {
        User user = resolveUser(username);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Override
    public List<Notification> getMesNonLues(String username) {
        User user = resolveUser(username);
        return notificationRepository.findByUserIdAndLueFalseOrderByCreatedAtDesc(user.getId());
    }

    @Override
    public long countNonLues(String username) {
        User user = resolveUser(username);
        return notificationRepository.countByUserIdAndLueFalse(user.getId());
    }

    @Override
    @Transactional
    public void marquerLue(Long notificationId, String username) {
        User user = resolveUser(username);
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(user.getId())) {
                n.setLue(true);
                notificationRepository.save(n);
            }
        });
    }

    @Override
    @Transactional
    public void marquerToutesLues(String username) {
        User user = resolveUser(username);
        notificationRepository.markAllAsRead(user.getId());
    }

    // ── Créateurs ──

    @Override
    public void notifierChangementStatut(Long userId, String stageSujet, String nouveauStatut) {
        String titre = "Statut de stage mis à jour";
        String message = "Le stage \"" + stageSujet + "\" est passé au statut : " + formatStatut(nouveauStatut);
        creer(userId, titre, message, "STAGE", "/stagiaire-dashboard");
    }

    @Override
    public void notifierConventionPrete(Long userId, String conventionNumero) {
        String titre = "Convention prête";
        String message = "La convention " + conventionNumero + " est disponible pour signature.";
        creer(userId, titre, message, "CONVENTION", "/stagiaire-dashboard");
    }

    @Override
    public void notifierNouvelleEvaluation(Long userId, String stageSujet, double note) {
        String titre = "Nouvelle évaluation";
        String message = "Vous avez reçu une évaluation de " + String.format("%.1f", note) + "/20 pour le stage \"" + stageSujet + "\".";
        creer(userId, titre, message, "EVALUATION", "/stagiaire-dashboard");
    }

    @Override
    public void notifierCandidatureRecue(Long userId, String candidatNom, String sujet) {
        String titre = "Nouvelle candidature";
        String message = candidatNom + " a postulé pour \"" + sujet + "\".";
        creer(userId, titre, message, "CANDIDATURE", "/candidatures");
    }

    @Override
    public void notifierAttestationPrete(Long userId, String numeroAttestation) {
        String titre = "Attestation prête";
        String message = "L'attestation " + numeroAttestation + " est disponible au téléchargement.";
        creer(userId, titre, message, "ATTESTATION", "/stagiaire-dashboard");
    }

    @Override
    public void notifierFicheRemplie(Long userId, String typeFiche, String encadrantNom) {
        String titre = "Fiche d'appréciation disponible";
        String message = "Votre encadrant " + encadrantNom + " a rempli la fiche " + typeFiche + ".";
        creer(userId, titre, message, "EVALUATION", "/stagiaire-dashboard");
    }

    @Override
    public void notifierSysteme(Long userId, String titre, String message) {
        creer(userId, titre, message, "SYSTEME", null);
    }

    // ── Helpers ──

    private void creer(Long userId, String titre, String message, String type, String lien) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        Notification n = Notification.builder()
                .user(user)
                .titre(titre)
                .message(message)
                .type(type)
                .lien(lien)
                .build();
        notificationRepository.save(n);
    }

    private User resolveUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + username));
    }

    private String formatStatut(String statut) {
        return switch (statut) {
            case "EN_ATTENTE" -> "En attente";
            case "VALIDEE" -> "Validé";
            case "REJETEE" -> "Rejeté";
            case "CONVENTION_GENEREE" -> "Convention générée";
            case "CONVENTION_SIGNEE" -> "Convention signée";
            case "EN_COURS" -> "En cours";
            case "EN_ATTENTE_EVALUATION" -> "En attente d'évaluation";
            case "TERMINE" -> "Terminé";
            case "ANNULE" -> "Annulé";
            default -> statut;
        };
    }
}
