package com.OCP.Gestion_Stages.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_candidature")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentCandidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidature_id", nullable = false)
    private Candidature candidature;

    @Column(name = "type_document", nullable = false, length = 50)
    private String typeDocument; // CONVENTION_ETABLISSEMENT, ASSURANCE, CIN, CV

    @Column(name = "nom_fichier", length = 255)
    private String nomFichier;

    @Column(name = "contenu")
    private byte[] contenu;

    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @Builder.Default
    @Column(name = "statut_ia", length = 30)
    private String statutIa = "NON_VERIFIE";

    @Column(name = "score_ia")
    private Integer scoreIa;

    @Column(name = "commentaire_ia", columnDefinition = "TEXT")
    private String commentaireIa;

    @PrePersist
    protected void onCreate() { this.uploadedAt = LocalDateTime.now(); }
}