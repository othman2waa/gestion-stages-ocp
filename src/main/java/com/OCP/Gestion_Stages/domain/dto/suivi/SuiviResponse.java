package com.OCP.Gestion_Stages.domain.dto.suivi;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SuiviResponse {
    private Long id;
    private Long stageId;
    private String stageSujet;
    private String stagiaireNom;
    private String encadrantNom;
    private Integer semaineNumero;
    private LocalDate dateSuivi;
    private Integer progression;
    private String commentaire;
    private String pointsPositifs;
    private String axesAmelioration;
    private BigDecimal note;
    private LocalDateTime createdAt;
}