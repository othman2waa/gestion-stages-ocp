package com.OCP.Gestion_Stages.domain.dto.pointage;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PointageResponse {
    private Long id;
    private Long stageId;
    private LocalDate date;
    private Boolean present;
    private String motif;
}
