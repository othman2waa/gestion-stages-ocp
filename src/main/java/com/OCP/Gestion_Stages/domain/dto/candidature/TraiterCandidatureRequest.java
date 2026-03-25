package com.OCP.Gestion_Stages.domain.dto.candidature;

import lombok.Data;

@Data
public class TraiterCandidatureRequest {
    private String statut; // ACCEPTEE ou REFUSEE
    private String commentaireRh;
    private Long encadrantId;
    private Long departementId;
}