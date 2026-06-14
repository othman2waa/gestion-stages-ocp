package com.OCP.Gestion_Stages.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "compte_stagiaire_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompteStagiaireLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stagiaire_id", nullable = false)
    private Stagiaire stagiaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private Stage stage;

    @Column(name = "action", nullable = false, length = 50)
    private String action; // DESACTIVATION_AUTO, DESACTIVATION_MANUELLE, REACTIVATION

    @Column(name = "date_fin_stage")
    private LocalDate dateFinStage;

    @Column(name = "date_action")
    private LocalDateTime dateAction;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @PrePersist
    protected void onCreate() {
        this.dateAction = LocalDateTime.now();
    }
}
