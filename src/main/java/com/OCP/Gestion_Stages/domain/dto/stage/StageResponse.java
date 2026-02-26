package com.OCP.Gestion_Stages.domain.dto.stage;



import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.enums.TypeStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageResponse {
    private Long id;
    private String stagiaireNom;
    private String stagiairePrenom;
    private String encadrantNom;
    private String departementNom;
    private String sujet;
    private TypeStage typeStage;
    private StageStatus statut;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDateTime createdAt;
}