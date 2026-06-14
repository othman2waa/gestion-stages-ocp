package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.ConventionRepository;
import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.domain.dto.convention.ConventionDTO;
import com.OCP.Gestion_Stages.domain.enums.ConventionStatus;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.model.Convention;
import com.OCP.Gestion_Stages.domain.model.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConventionServiceImpl — génération de la convocation")
class ConventionServiceImplTest {

    @Mock private ConventionRepository conventionRepository;
    @Mock private StageRepository stageRepository;

    @InjectMocks private ConventionServiceImpl service;

    @Test
    @DisplayName("generer : crée la convention et passe le stage à CONVENTION_GENEREE")
    void generer_cree_et_avance_le_stage() {
        Stage stage = new Stage();
        stage.setId(15L);
        stage.setStatut(StageStatus.VALIDEE);
        when(stageRepository.findById(15L)).thenReturn(Optional.of(stage));
        when(conventionRepository.findByStageId(15L)).thenReturn(Optional.empty());
        when(conventionRepository.save(any(Convention.class))).thenAnswer(i -> i.getArgument(0));

        ConventionDTO dto = service.generer(15L);

        assertThat(stage.getStatut()).isEqualTo(StageStatus.CONVENTION_GENEREE);
        assertThat(dto.getNumero()).isEqualTo("CONV-15-" + LocalDate.now().getYear());
        verify(conventionRepository).save(any(Convention.class));
    }

    @Test
    @DisplayName("generer est idempotent : renvoie la convention existante, sans doublon ni régression de statut")
    void generer_idempotent() {
        Stage stage = new Stage();
        stage.setId(15L);
        stage.setStatut(StageStatus.CONVENTION_SIGNEE);
        Convention existante = Convention.builder()
                .id(99L).stage(stage)
                .statut(ConventionStatus.SIGNEE)
                .numero("CONV-15-2026")
                .build();
        when(stageRepository.findById(15L)).thenReturn(Optional.of(stage));
        when(conventionRepository.findByStageId(15L)).thenReturn(Optional.of(existante));

        ConventionDTO dto = service.generer(15L);

        assertThat(dto.getId()).isEqualTo(99L);
        verify(conventionRepository, never()).save(any());
        assertThat(stage.getStatut()).isEqualTo(StageStatus.CONVENTION_SIGNEE); // pas de régression
    }
}
