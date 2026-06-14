package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.AttestationPdfService;
import com.OCP.Gestion_Stages.Service.ArchiveStageService;
import com.OCP.Gestion_Stages.Service.EmailService;
import com.OCP.Gestion_Stages.domain.dto.stage.StageResponse;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.model.Stage;
import com.OCP.Gestion_Stages.exeptions.BusinessException;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StageServiceImpl — cycle de vie et CRUD des stages")
class StageServiceImplTest {

    @Mock private StageRepository stageRepository;
    @Mock private StagiaireRepository stagiaireRepository;
    @Mock private EncadrantRepository encadrantRepository;
    @Mock private DepartementRepository departementRepository;
    @Mock private ArchiveStageService archiveStageService;
    @Mock private AttestationStageRepository attestationRepository;
    @Mock private AttestationPdfService attestationPdfService;
    @Mock private EmailService emailService;

    @InjectMocks private StageServiceImpl service;

    private Stage stage(StageStatus statut) {
        Stage s = new Stage();
        s.setId(1L); s.setSujet("Sujet"); s.setStatut(statut);
        return s;
    }

    @Test
    @DisplayName("updateStatut applique une transition légitime")
    void updateStatutValide() {
        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage(StageStatus.EN_COURS)));
        when(stageRepository.save(any(Stage.class))).thenAnswer(i -> i.getArgument(0));
        StageResponse r = service.updateStatut(1L, StageStatus.EN_ATTENTE_EVALUATION);
        assertThat(r.getStatut()).isEqualTo(StageStatus.EN_ATTENTE_EVALUATION);
    }

    @Test
    @DisplayName("updateStatut rejette une transition illégitime")
    void updateStatutInvalide() {
        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage(StageStatus.EN_ATTENTE)));
        assertThatThrownBy(() -> service.updateStatut(1L, StageStatus.TERMINE))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("findById sur un id inconnu lève ResourceNotFoundException")
    void findByIdIntrouvable() {
        when(stageRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete sur un id inconnu lève ResourceNotFoundException")
    void deleteIntrouvable() {
        when(stageRepository.existsById(9L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("findAll mappe les stages")
    void findAllMappe() {
        when(stageRepository.findAll()).thenReturn(List.of(stage(StageStatus.EN_COURS)));
        assertThat(service.findAll()).hasSize(1);
    }
}
