package com.OCP.Gestion_Stages.domain.dto.stagiaire;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StagiaireResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String cin;
    private String filiere;
    private String niveau;
    private Long etablissementId;
    private String etablissementNom;
    private LocalDateTime createdAt;
}