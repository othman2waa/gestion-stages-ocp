package com.OCP.Gestion_Stages.Service.interfaces;



public interface NotificationService {
    void sendStageAcceptedEmail(String to, String stagiaireNom, String sujet);
    void sendStageRefusedEmail(String to, String stagiaireNom);
    void sendConventionReadyEmail(String to, String stagiaireNom, String conventionNumero);
    void sendEvaluationReminderEmail(String to, String encadrantNom, String stagiaireNom);
}