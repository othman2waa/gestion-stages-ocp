package com.OCP.Gestion_Stages.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stagiaire")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Stagiaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String telephone;

    @Column(unique = true, length = 20)
    private String cin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @Column(length = 100)
    private String filiere;

    @Column(length = 50)
    private String niveau;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    @ManyToOne
    @JoinColumn(name = "departement_id")
    private Departement departement;

    @Column(name = "adresse_personnelle", columnDefinition = "TEXT")
    private String adressePersonnelle;

    @Column(name = "ville_etablissement", length = 100)
    private String villeEtablissement;

    @Column(name = "adresse_logement_stage", columnDefinition = "TEXT")
    private String adresseLogementStage;

    @Column(name = "contact_urgence_nom", length = 150)
    private String contactUrgenceNom;

    @Column(name = "contact_urgence_tel", length = 20)
    private String contactUrgenceTel;

    @Column(name = "service_accueil", length = 50)
    private String serviceAccueil;

    @Column(name = "diplome", length = 50)
    private String diplome;

    @Column(name = "regles_acceptees")
    private Boolean reglesAcceptees = false;

    @Column(name = "observation", columnDefinition = "TEXT")
    private String observation;

    @Column(name = "fiche_renseignement_completee")
    private Boolean ficheRenseignementCompletee = false;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "sexe", length = 10)
    private String sexe;

    @Column(name = "nationalite", length = 50)
    private String nationalite;

    @Column(name = "lieu_naissance", length = 100)
    private String lieuNaissance;

    @Column(name = "date_desactivation_prevue")
    private LocalDate dateDesactivationPrevue;

}