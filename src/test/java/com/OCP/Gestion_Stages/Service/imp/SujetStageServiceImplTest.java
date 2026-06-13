package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.domain.dto.sujet.SujetStageDTO;
import com.OCP.Gestion_Stages.domain.model.SujetStage;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SujetStageServiceImpl — validation/refus des sujets")
class SujetStageServiceImplTest {

    @Mock private SujetStageRepository sujetRepository;
    @Mock private EncadrantRepository encadrantRepository;
    @Mock private DepartementRepository departementRepository;
    @Mock private StageRepository stageRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private SujetStageServiceImpl service;

    private SujetStage sujet() {
        SujetStage s = new SujetStage();
        s.setId(1L); s.setTitre("Sujet PFE"); s.setStatut("PROPOSE");
        return s;
    }

    @Test
    @DisplayName("valider passe le sujet au statut VALIDE")
    void validerOk() {
        when(sujetRepository.findById(1L)).thenReturn(Optional.of(sujet()));
        when(sujetRepository.save(any(SujetStage.class))).thenAnswer(i -> i.getArgument(0));
        SujetStageDTO dto = service.valider(1L, "rh.admin");
        assertThat(dto.getStatut()).isEqualTo("VALIDE");
    }

    @Test
    @DisplayName("refuser passe le sujet au statut REFUSE")
    void refuserOk() {
        when(sujetRepository.findById(1L)).thenReturn(Optional.of(sujet()));
        when(sujetRepository.save(any(SujetStage.class))).thenAnswer(i -> i.getArgument(0));
        SujetStageDTO dto = service.refuser(1L);
        assertThat(dto.getStatut()).isEqualTo("REFUSE");
    }

    @Test
    @DisplayName("valider sur un id inconnu lève ResourceNotFoundException")
    void validerIntrouvable() {
        when(sujetRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.valider(9L, "rh")).isInstanceOf(ResourceNotFoundException.class);
    }
}
