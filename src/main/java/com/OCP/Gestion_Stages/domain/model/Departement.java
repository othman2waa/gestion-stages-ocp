package com.OCP.Gestion_Stages.domain.model;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "departement")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder

public class Departement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(length = 100)
    private String responsable;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String telephone;

    @Column(length = 100)
    private String localisation;

    @Column
    private Boolean actif = true;
}