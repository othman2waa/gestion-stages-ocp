package com.OCP.Gestion_Stages.domain.dto.suivi;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SuiviRequest {
    private Long stageId;
    private Integer semaineNumero;
    private LocalDate dateSuivi;
    private Integer progression;
    private String commentaire;
    private String pointsPositifs;
    private String axesAmelioration;
    private BigDecimal note;
}
