package com.OCP.Gestion_Stages.domain.dto.attestation;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class AttestationDTO {
    private Long id;
    private String statut;
    private LocalDateTime dateDemande;
    private LocalDateTime dateTraitement;
    private String traitePar;
    private String numeroAttestation;
    private String commentaire;
    private Long stageId;
    private String stageSujet;
    private String stagiaireNom;
}