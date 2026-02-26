package com.OCP.Gestion_Stages.domain.dto.convention;


import com.OCP.Gestion_Stages.domain.enums.ConventionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConventionResponse {
    private Long id;
    private Long stageId;
    private String stagiaireNom;
    private String numero;
    private ConventionStatus statut;
    private String cheminFichier;
    private LocalDate dateEmission;
    private LocalDateTime createdAt;
}