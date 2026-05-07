package com.OCP.Gestion_Stages.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "archive_stage")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArchiveStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_id", unique = true)
    private Long stageId;

    // Stagiaire
    @Column(name = "stagiaire_nom")       private String stagiaireNom;
    @Column(name = "stagiaire_prenom")    private String stagiairePrenom;
    @Column(name = "stagiaire_email")     private String stagiaireEmail;
    @Column(name = "stagiaire_filiere")   private String stagiaireFiliere;
    @Column(name = "stagiaire_niveau")    private String stagiaireNiveau;
    @Column(name = "stagiaire_etablissement") private String stagiaireEtablissement;

    // Encadrant
    @Column(name = "encadrant_nom")       private String encadrantNom;
    @Column(name = "encadrant_prenom")    private String encadrantPrenom;
    @Column(name = "encadrant_email")     private String encadrantEmail;

    // Stage
    @Column(name = "departement_nom")     private String departementNom;
    @Column(name = "sujet", columnDefinition = "TEXT") private String sujet;
    @Column(name = "type_stage")          private String typeStage;
    @Column(name = "date_debut")          private LocalDate dateDebut;
    @Column(name = "date_fin")            private LocalDate dateFin;

    // Résultats
    @Column(name = "note_finale", precision = 4, scale = 2) private BigDecimal noteFinale;
    @Column(name = "mention")             private String mention;

    // Archive
    @Column(name = "annee_stage")         private Integer anneeStage;
    @Column(name = "date_archivage")      private LocalDateTime dateArchivage;
    @Column(name = "archive_par")         private String archivePar;
}