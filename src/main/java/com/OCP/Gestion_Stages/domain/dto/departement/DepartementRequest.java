package com.OCP.Gestion_Stages.domain.dto.departement;

import lombok.Data;

@Data
public class DepartementRequest {
    private String code;
    private String nom;
    private String responsable;
    private String description;
    private String email;
    private String telephone;
    private String localisation;
    private Boolean actif;
}