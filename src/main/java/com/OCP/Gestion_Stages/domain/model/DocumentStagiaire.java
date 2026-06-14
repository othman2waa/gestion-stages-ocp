package com.OCP.Gestion_Stages.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_stagiaire",
       uniqueConstraints = @UniqueConstraint(columnNames = {"stagiaire_id", "type_document"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentStagiaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stagiaire_id", nullable = false)
    private Stagiaire stagiaire;

    @Column(name = "type_document", nullable = false, length = 50)
    private String typeDocument; // CV, CIN, PHOTO, DIPLOME, ASSURANCE, AUTRE

    @Column(name = "nom_fichier", nullable = false, length = 255)
    private String nomFichier;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "taille")
    private Long taille;

    // Legacy : contenu en base (anciennes lignes). Les nouvelles utilisent cheminFichier.
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "contenu")
    private byte[] contenu;

    // Chemin relatif du fichier sur disque (stockage actuel).
    @Column(name = "chemin_fichier", length = 500)
    private String cheminFichier;

    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}
