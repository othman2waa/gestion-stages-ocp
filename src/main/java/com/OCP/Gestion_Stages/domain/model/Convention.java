package com.OCP.Gestion_Stages.domain.model;

import com.OCP.Gestion_Stages.domain.enums.ConventionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "convention")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Convention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false, unique = true)
    private Stage stage;

    @Column(unique = true, length = 50)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ConventionStatus statut = ConventionStatus.BROUILLON;

    @Column(name = "chemin_fichier", length = 255)
    private String cheminFichier;

    @Column(name = "date_emission")
    private LocalDate dateEmission;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}