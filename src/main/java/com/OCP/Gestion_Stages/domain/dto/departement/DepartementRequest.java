package com.OCP.Gestion_Stages.domain.dto.departement;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepartementRequest {
    @NotBlank(message = "Le code du département est obligatoire")
    @Size(max = 50, message = "Le code ne doit pas dépasser 50 caractères")
    private String code;

    @NotBlank(message = "Le nom du département est obligatoire")
    private String nom;
    private String responsable;
    private String description;

    @Email(message = "L'adresse email du département n'est pas valide")
    private String email;
    private String telephone;
    private String localisation;
    private Boolean actif;
}