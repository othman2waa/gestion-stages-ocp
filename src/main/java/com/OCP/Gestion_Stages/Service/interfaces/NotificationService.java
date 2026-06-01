package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.model.Notification;
import java.util.List;

public interface NotificationService {

    // CRUD
    List<Notification> getMesNotifications(String username);
    List<Notification> getMesNonLues(String username);
    long countNonLues(String username);
    void marquerLue(Long notificationId, String username);
    void marquerToutesLues(String username);

    // Créateurs de notifications
    void notifierChangementStatut(Long userId, String stageSujet, String nouveauStatut);
    void notifierConventionPrete(Long userId, String conventionNumero);
    void notifierNouvelleEvaluation(Long userId, String stageSujet, double note);
    void notifierCandidatureRecue(Long userId, String candidatNom, String sujet);
    void notifierAttestationPrete(Long userId, String numeroAttestation);
    void notifierFicheRemplie(Long userId, String typeFiche, String encadrantNom);
    void notifierSysteme(Long userId, String titre, String message);
}
