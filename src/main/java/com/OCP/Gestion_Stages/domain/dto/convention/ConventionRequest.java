package com.OCP.Gestion_Stages.domain.dto.convention;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ConventionRequest {

    @NotNull(message = "Le stage est obligatoire")
    private Long stageId;

    private String numero;
    private LocalDate dateEmission;
}