package com.OCP.Gestion_Stages.domain.dto.encadrant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EncadrantRequest {

    @NotBlank
    private String nom;

    @NotBlank
    private String prenom;

    @Email
    @NotBlank
    private String email;

    private String fonction;

    private Long departementId;

    private Long userId;
}