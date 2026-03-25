package com.OCP.Gestion_Stages.domain.dto.candidature;

import lombok.Data;

@Data
public class CandidatureRequest {
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String filiere;
    private String niveau;
    private String etablissement;
    private String sujetSouhaite;
    private String departementSouhaite;
    private String message;
}