package com.OCP.Gestion_Stages.domain.model;

import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stage_historique")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_precedent")
    private StageStatus statutPrecedent;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_nouveau", nullable = false)
    private StageStatus statutNouveau;

    @Column(name = "commentaire")
    private String commentaire;

    @Column(name = "modifie_par")
    private String modifiePar;

    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;

    @PrePersist
    public void prePersist() {
        this.dateModification = LocalDateTime.now();
    }
}