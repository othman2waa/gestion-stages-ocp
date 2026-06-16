package com.OCP.Gestion_Stages.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "candidature")
@Data
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    private String email;

    private String telephone;
    private String filiere;
    private String niveau;
    private String etablissement;

    @Column(name = "sujet_souhaite", columnDefinition = "TEXT")
    private String sujetSouhaite;

    @Column(name = "departement_souhaite")
    private String departementSouhaite;

    // ── Nouveau : lien FK vers departement
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departement_id")
    private Departement departement;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String statut = "EN_ATTENTE";

    // Statuts possibles :
    // EN_ATTENTE, VU_ENCADRANT, MEETING_PLANIFIE, ACCEPTEE_ENCADRANT,
    // REFUSEE_ENCADRANT, DOCUMENTS_REQUIS, DOCUMENTS_SOUMIS,
    // VERIFICATION_IA, ACCEPTEE_RH, REFUSEE_RH, CONVOCATION_ENVOYEE

    @Column(name = "commentaire_rh", columnDefinition = "TEXT")
    private String commentaireRh;

    // LAZY : ne charge le CV qu'au download, pas dans les listes de candidatures.
    // Effectif uniquement si l'enhancement bytecode Hibernate est activé (voir pom.xml).
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "cv_contenu", columnDefinition = "bytea")
    private byte[] cvContenu;

    @Column(name = "cv_nom_fichier")
    private String cvNomFichier;

    // Chemin relatif du CV sur disque (stockage actuel ; cvContenu = legacy en base).
    @Column(name = "cv_chemin", length = 500)
    private String cvChemin;

    // ── Précalcul IA du CV (fait en tâche de fond au dépôt → recherche encadrant quasi instantanée) ──
    @Column(name = "cv_texte", columnDefinition = "TEXT")
    private String cvTexte;            // texte du CV extrait (PDFBox/OCR)

    @Column(name = "cv_embedding", columnDefinition = "TEXT")
    private String cvEmbedding;        // vecteur d'embedding du CV, sérialisé en JSON

    @Column(name = "cv_traite")
    private Boolean cvTraite = false;  // le traitement IA de fond a-t-il été effectué ?

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "traite_at")
    private LocalDateTime traiteAt;

    @Column(name = "traite_par")
    private String traitePar;

    @Column(name = "annonce_id")
    private Long annonceId;

    @Column(name = "score_matching")
    private Integer scoreMatching;

    @Column(name = "competences_extraites", columnDefinition = "TEXT")
    private String competencesExtraites;

    @Column(name = "specialite", length = 100)
    private String specialite;
    // ── Nouveaux champs
    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "password_temp", length = 100)
    private String passwordTemp;

    @Column(name = "date_meeting")
    private LocalDateTime dateMeeting;

    @Column(name = "statut_meeting", length = 30)
    private String statutMeeting = "SANS_MEETING";

    @Column(name = "note_encadrant", columnDefinition = "TEXT")
    private String noteEncadrant;

    @Column(name = "convocation_envoyee")
    private Boolean convocationEnvoyee = false;

    @Column(name = "date_convocation")
    private LocalDateTime dateConvocation;

    // ── Documents uploadés
    @OneToMany(mappedBy = "candidature", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DocumentCandidature> documents;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}