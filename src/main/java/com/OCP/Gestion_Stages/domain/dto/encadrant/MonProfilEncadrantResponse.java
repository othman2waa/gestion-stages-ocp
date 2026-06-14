package com.OCP.Gestion_Stages.domain.dto.encadrant;

import lombok.Data;
import java.util.List;

@Data
public class MonProfilEncadrantResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String fonction;
    private String departementNom;
    private int nombreStagiaires;
    private int nombreSuivis;
    private List<StagiaireInfoDto> stagiaires;

    @Data
    public static class StagiaireInfoDto {
        private Long stageId;
        private String stagiaireNom;
        private String stagiairePrenom;
        private String stagiaireEmail;
        private String stagiaireFiliere;
        private String stagiaireNiveau;
        private String stagiaireEtablissement;
        private String sujet;
        private String typeStage;
        private String statut;
        private String dateDebut;
        private String dateFin;
        private int progression;
        private int nombreSuivis;
    }
}