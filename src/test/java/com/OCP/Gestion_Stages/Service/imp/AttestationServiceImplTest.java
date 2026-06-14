package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.AttestationStageRepository;
import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.Repository.UserRepository;
import com.OCP.Gestion_Stages.Service.interfaces.NotificationService;
import com.OCP.Gestion_Stages.domain.dto.attestation.AttestationDTO;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.model.AttestationStage;
import com.OCP.Gestion_Stages.domain.model.Stage;
import com.OCP.Gestion_Stages.domain.model.Stagiaire;
import com.OCP.Gestion_Stages.exeptions.BusinessException;
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
@DisplayName("AttestationServiceImpl — demande et traitement des attestations")
class AttestationServiceImplTest {

    @Mock private AttestationStageRepository attestationRepository;
    @Mock private StageRepository stageRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private AttestationServiceImpl service;

    private Stage stage(StageStatus statut) {
        Stage s = new Stage();
        s.setId(7L);
        s.setStatut(statut);
        Stagiaire st = new Stagiaire();
        st.setNom("Test"); st.setPrenom("Stagiaire");
        s.setStagiaire(st);
        return s;
    }

    @Test
    @DisplayName("demander refuse si le stage n'est pas terminé")
    void demanderRefuseSiNonTermine() {
        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage(StageStatus.EN_COURS)));
        when(attestationRepository.findByStageId(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.demander(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("terminé");
    }

    @Test
    @DisplayName("demander refuse si une attestation existe déjà")
    void demanderRefuseSiDejaDemandee() {
        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage(StageStatus.TERMINE)));
        when(attestationRepository.findByStageId(1L)).thenReturn(Optional.of(new AttestationStage()));
        assertThatThrownBy(() -> service.demander(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà");
    }

    @Test
    @DisplayName("demander crée une attestation EN_ATTENTE pour un stage terminé")
    void demanderOk() {
        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage(StageStatus.TERMINE)));
        when(attestationRepository.findByStageId(1L)).thenReturn(Optional.empty());
        when(attestationRepository.save(any(AttestationStage.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByRole(any())).thenReturn(List.of());
        AttestationDTO dto = service.demander(1L);
        assertThat(dto.getStatut()).isEqualTo("EN_ATTENTE");
    }

    @Test
    @DisplayName("approuver fixe le statut APPROUVEE et un numéro")
    void approuver() {
        AttestationStage a = new AttestationStage();
        a.setStage(stage(StageStatus.TERMINE));
        when(attestationRepository.findById(5L)).thenReturn(Optional.of(a));
        when(attestationRepository.save(any(AttestationStage.class))).thenAnswer(i -> i.getArgument(0));
        AttestationDTO dto = service.approuver(5L, "rh.admin");
        assertThat(dto.getStatut()).isEqualTo("APPROUVEE");
        assertThat(dto.getNumeroAttestation()).startsWith("ATT-7-");
    }

    @Test
    @DisplayName("refuser fixe le statut REFUSEE avec un commentaire")
    void refuser() {
        AttestationStage a = new AttestationStage();
        a.setStage(stage(StageStatus.TERMINE));
        when(attestationRepository.findById(5L)).thenReturn(Optional.of(a));
        when(attestationRepository.save(any(AttestationStage.class))).thenAnswer(i -> i.getArgument(0));
        AttestationDTO dto = service.refuser(5L, "Dossier incomplet", "rh.admin");
        assertThat(dto.getStatut()).isEqualTo("REFUSEE");
        assertThat(dto.getCommentaire()).contains("incomplet");
    }
}
