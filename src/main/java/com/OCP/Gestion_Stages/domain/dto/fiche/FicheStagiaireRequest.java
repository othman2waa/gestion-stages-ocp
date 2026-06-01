package com.OCP.Gestion_Stages.domain.dto.fiche;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FicheStagiaireRequest {

    @NotNull
    private Long stageId;

    @NotNull @Min(1) @Max(5)
    private Integer assiduite;

    @NotNull @Min(1) @Max(5)
    private Integer ponctualite;

    @NotNull @Min(1) @Max(5)
    private Integer comportementProfessionnel;

    @NotNull @Min(1) @Max(5)
    private Integer espritEquipe;

    @NotNull @Min(1) @Max(5)
    private Integer communication;

    @NotNull @Min(1) @Max(5)
    private Integer adaptation;

    @NotNull
    private String recommandationEmbauche;

    private String commentaires;
}
