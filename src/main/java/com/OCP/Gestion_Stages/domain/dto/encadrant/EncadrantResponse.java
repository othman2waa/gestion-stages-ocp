package com.OCP.Gestion_Stages.domain.dto.encadrant;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EncadrantResponse {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String fonction;
    private String departementNom;
    private Long departementId;
    private Long userId;
    private LocalDateTime createdAt;
}