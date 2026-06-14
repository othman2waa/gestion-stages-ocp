package com.OCP.Gestion_Stages.domain.dto;

import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import lombok.Data;

@Data
public class WorkflowRequest {
    private StageStatus nouveauStatut;
    private String commentaire;
}