package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.interfaces.NotificationService;
import com.OCP.Gestion_Stages.domain.dto.evaluation.EvaluationRequest;
import com.OCP.Gestion_Stages.domain.dto.evaluation.EvaluationResponse;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.enums.TypeEvaluation;
import com.OCP.Gestion_Stages.domain.model.Encadrant;
import com.OCP.Gestion_Stages.domain.model.Evaluation;
import com.OCP.Gestion_Stages.domain.model.Stage;
import com.OCP.Gestion_Stages.exeptions.BusinessException;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EvaluationServiceImpl — création et garde-fous des évaluations")
class EvaluationServiceImplTest {

    @Mock private EvaluationRepository evaluationRepository;
    @Mock private StageRepository stageRepository;
    @Mock private EncadrantRepository encadrantRepository;
    @Mock private UserRepository userRepository;
    @Mock private StagiaireRepository stagiaireRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private EvaluationServiceImpl service;

    private Encadrant enc(Long id) {
        Encadrant e = new Encadrant();
        e.setId(id); e.setNom("Encadrant"); e.setPrenom("Test");
        return e;
    }

    private Stage stage(Long encadrantId, StageStatus statut) {
        Stage s = new Stage();
        s.setId(1L); s.setSujet("Sujet"); s.setStatut(statut);
        s.setEncadrant(enc(encadrantId));
        return s;
    }

    private EvaluationRequest req(Long stageId, Long encId, TypeEvaluation type) {
        EvaluationRequest r = new EvaluationRequest();
        r.setStageId(stageId); r.setEncadrantId(encId);
        r.setNote(15.0); r.setCommentaire("Bon travail"); r.setTypeEvaluation(type);
        return r;
    }

    @Test
    @DisplayName("findById sur un id inconnu lève ResourceNotFoundException")
    void findByIdIntrouvable() {
        when(evaluationRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("création rejetée si l'encadrant n'est pas celui du stage")
    void createRejeteSiEncadrantNonAffecte() {
        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage(1L, StageStatus.EN_COURS)));
        when(encadrantRepository.findById(2L)).thenReturn(Optional.of(enc(2L)));
        assertThatThrownBy(() -> service.create(req(1L, 2L, TypeEvaluation.MI_PARCOURS)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("évaluation FIN_STAGE rejetée si la transition d'état est illégitime")
    void createFinStageTransitionInvalide() {
        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage(1L, StageStatus.EN_ATTENTE)));
        when(encadrantRepository.findById(1L)).thenReturn(Optional.of(enc(1L)));
        assertThatThrownBy(() -> service.create(req(1L, 1L, TypeEvaluation.FIN_STAGE)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("création valide enregistre l'évaluation")
    void createOk() {
        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage(1L, StageStatus.EN_COURS)));
        when(encadrantRepository.findById(1L)).thenReturn(Optional.of(enc(1L)));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(i -> i.getArgument(0));
        EvaluationResponse resp = service.create(req(1L, 1L, TypeEvaluation.MI_PARCOURS));
        assertThat(resp.getNote()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("findByStage renvoie la liste mappée")
    void findByStage() {
        when(evaluationRepository.findByStageId(1L)).thenReturn(List.of());
        assertThat(service.findByStage(1L)).isEmpty();
    }
}
