package com.OCP.Gestion_Stages.domain.dto.stage;


import com.OCP.Gestion_Stages.domain.enums.TypeStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class StageRequest {

    @NotNull(message = "Le stagiaire est obligatoire")
    private Long stagiaireId;

    private Long encadrantId;
    private Long departementId;

    @NotBlank(message = "Le sujet est obligatoire")
    private String sujet;

    @NotNull(message = "Le type de stage est obligatoire")
    private TypeStage typeStage;

    private LocalDate dateDebut;
    private LocalDate dateFin;
}