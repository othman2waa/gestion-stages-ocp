package com.OCP.Gestion_Stages.domain.dto.stagiaire;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentStagiaireResponse {
    private Long id;
    private String typeDocument;
    private String nomFichier;
    private String contentType;
    private Long taille;
    private LocalDateTime uploadedAt;
}
