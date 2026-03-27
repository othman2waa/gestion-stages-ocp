package com.OCP.Gestion_Stages.domain.dto.candidature;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CandidatureResponse {
    private Long id;
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
    private String statut;
    private String commentaireRh;
    private String cvNomFichier;
    private boolean hasCv;
    private LocalDateTime createdAt;
    private LocalDateTime traiteAt;
    private String traitePar;
    private Integer scoreMatching;
    private Long annonceId;
}