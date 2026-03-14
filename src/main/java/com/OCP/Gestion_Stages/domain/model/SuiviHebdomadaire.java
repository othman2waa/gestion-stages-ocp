package com.OCP.Gestion_Stages.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "suivis_hebdomadaires")
@Data
public class SuiviHebdomadaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encadrant_id", nullable = false)
    private User encadrant;

    @Column(name = "semaine_numero")
    private Integer semaineNumero;

    @Column(name = "date_suivi")
    private LocalDate dateSuivi;

    private Integer progression;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "points_positifs", columnDefinition = "TEXT")
    private String pointsPositifs;

    @Column(name = "axes_amelioration", columnDefinition = "TEXT")
    private String axesAmelioration;

    private BigDecimal note;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}