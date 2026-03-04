package com.OCP.Gestion_Stages.domain.dto.stagiaire;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StagiaireRequest {
    @NotBlank
    private String nom;
    @NotBlank
    private String prenom;
    @Email @NotBlank
    private String email;
    private String telephone;
    private String cin;
    private String filiere;
    private String niveau;
    private Long etablissementId;
}