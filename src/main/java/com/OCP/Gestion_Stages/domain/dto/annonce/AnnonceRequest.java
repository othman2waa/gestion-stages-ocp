package com.OCP.Gestion_Stages.domain.dto.annonce;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AnnonceRequest {
    private String titre;
    private String description;
    private String competencesRequises;
    private String departement;
    private String typeStage;
    private String niveauRequis;
    private String filiereRequise;
    private Integer nombrePostes;
    private LocalDate dateLimite;
    private Boolean actif;
}