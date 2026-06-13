package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.interfaces.NotificationService;
import com.OCP.Gestion_Stages.domain.dto.fiche.FicheStageRequest;
import com.OCP.Gestion_Stages.domain.model.Encadrant;
import com.OCP.Gestion_Stages.domain.model.Stage;
import com.OCP.Gestion_Stages.domain.model.User;
import com.OCP.Gestion_Stages.exeptions.UnauthorizedException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FicheAppreciationServiceImpl — garde-fous de remplissage des fiches")
class FicheAppreciationServiceImplTest {

    @Mock private FicheAppreciationStageRepository ficheStageRepo;
    @Mock private FicheAppreciationStagiaireRepository ficheStagiaireRepo;
    @Mock private StageRepository stageRepository;
    @Mock private EncadrantRepository encadrantRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private FicheAppreciationServiceImpl service;

    private Encadrant enc(Long id) { Encadrant e = new Encadrant(); e.setId(id); return e; }

    @Test
    @DisplayName("création rejetée si une fiche existe déjà pour le stage")
    void createRejeteSiDejaExistante() {
        Stage s = new Stage(); s.setId(1L);
        when(stageRepository.findById(1L)).thenReturn(Optional.of(s));
        when(ficheStageRepo.existsByStageId(1L)).thenReturn(true);
        FicheStageRequest req = new FicheStageRequest();
        req.setStageId(1L);
        assertThatThrownBy(() -> service.createFicheStage(req, "enc"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("création rejetée si l'encadrant n'est pas celui du stage")
    void createRejeteSiPasSonStage() {
        Stage s = new Stage(); s.setId(1L); s.setEncadrant(enc(1L));
        when(stageRepository.findById(1L)).thenReturn(Optional.of(s));
        when(ficheStageRepo.existsByStageId(1L)).thenReturn(false);
        User u = new User(); u.setId(99L);
        when(userRepository.findByUsername("enc")).thenReturn(Optional.of(u));
        when(encadrantRepository.findByUserId(99L)).thenReturn(Optional.of(enc(2L)));
        FicheStageRequest req = new FicheStageRequest();
        req.setStageId(1L);
        assertThatThrownBy(() -> service.createFicheStage(req, "enc"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("getAllFichesStage renvoie la liste mappée")
    void getAllVide() {
        when(ficheStageRepo.findAll()).thenReturn(List.of());
        assertThat(service.getAllFichesStage()).isEmpty();
    }
}
