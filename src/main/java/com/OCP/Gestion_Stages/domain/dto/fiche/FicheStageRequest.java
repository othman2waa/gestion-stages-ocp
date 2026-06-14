package com.OCP.Gestion_Stages.domain.dto.fiche;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FicheStageRequest {

    @NotNull
    private Long stageId;

    @NotNull @Min(1) @Max(5)
    private Integer noteGlobale;

    @NotNull @Min(1) @Max(5)
    private Integer qualiteTravail;

    @NotNull @Min(1) @Max(5)
    private Integer respectDelais;

    @NotNull @Min(1) @Max(5)
    private Integer initiative;

    @NotNull @Min(1) @Max(5)
    private Integer qualiteRapport;

    @NotNull @Min(1) @Max(5)
    private Integer competencesTechniques;

    @NotNull
    private String recommandation;

    private String commentaires;
}
