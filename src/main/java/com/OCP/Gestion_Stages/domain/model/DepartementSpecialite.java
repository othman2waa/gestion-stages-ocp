package com.OCP.Gestion_Stages.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "departement_specialite")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DepartementSpecialite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departement_id", nullable = false)
    private Departement departement;

    @Column(nullable = false, length = 100)
    private String nom;
}