package com.OCP.Gestion_Stages.domain.dto.stagiaire;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StagiaireResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String cin;
    private String etablissementNom;
    private String filiere;
    private String niveau;
    private LocalDateTime createdAt;
}