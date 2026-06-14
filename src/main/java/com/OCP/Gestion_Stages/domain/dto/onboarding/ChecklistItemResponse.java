package com.OCP.Gestion_Stages.domain.dto.onboarding;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChecklistItemResponse {
    private Long id;
    private String etape;
    private String categorie;
    private String description;
    private Boolean completed;
    private LocalDateTime completedAt;
    private String completedBy;
    private Integer ordre;
    private Long stagiaireId;
    private String stagiaireNom;
}