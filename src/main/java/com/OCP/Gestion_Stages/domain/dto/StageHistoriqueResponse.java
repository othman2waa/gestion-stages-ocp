package com.OCP.Gestion_Stages.domain.dto;

import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StageHistoriqueResponse {
    private Long id;
    private StageStatus statutPrecedent;
    private StageStatus statutNouveau;
    private String commentaire;
    private String modifiePar;
    private LocalDateTime dateModification;
}