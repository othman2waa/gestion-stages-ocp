package com.OCP.Gestion_Stages.domain.dto.pointage;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PointageRequest {
    private LocalDate date;
    private Boolean present;
    private String motif;
}
