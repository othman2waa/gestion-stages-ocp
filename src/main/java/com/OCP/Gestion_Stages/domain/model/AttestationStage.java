package com.OCP.Gestion_Stages.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "attestation_stage")
@Data
public class AttestationStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private Stage stage;

    @Column(nullable = false)
    private String statut = "EN_ATTENTE";

    @Column(name = "date_demande")
    private LocalDateTime dateDemande;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;

    @Column(name = "traite_par")
    private String traitePar;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "numero_attestation")
    private String numeroAttestation;

    @PrePersist
    protected void onCreate() { this.dateDemande = LocalDateTime.now(); }
}