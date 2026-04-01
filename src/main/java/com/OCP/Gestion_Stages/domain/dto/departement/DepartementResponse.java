package com.OCP.Gestion_Stages.domain.dto.departement;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DepartementResponse {
    private Long id;
    private String code;
    private String nom;
    private String responsable;
    private String description;
    private String email;
    private String telephone;
    private String localisation;
    private Boolean actif;
    private LocalDateTime createdAt;
    private int nombreEncadrants;
    private int nombreStages;
    private int nombreStagesEnCours;
    private int nombreStagiaires;
}