package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.AnnonceStageRepository;
import com.OCP.Gestion_Stages.Repository.CandidatureRepository;
import com.OCP.Gestion_Stages.domain.dto.annonce.AnnonceRequest;
import com.OCP.Gestion_Stages.domain.dto.annonce.AnnonceResponse;
import com.OCP.Gestion_Stages.domain.model.AnnonceStage;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AnnonceServiceImpl — CRUD annonces")
class AnnonceServiceImplTest {

    @Mock private AnnonceStageRepository annonceRepository;
    @Mock private CandidatureRepository candidatureRepository;

    @InjectMocks private AnnonceServiceImpl service;

    @BeforeEach
    void setUp() {
        when(candidatureRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
    }

    @Test
    @DisplayName("create sauvegarde l'annonce avec l'auteur connecté")
    void create_avec_auteur() {
        AnnonceRequest req = new AnnonceRequest();
        req.setTitre("Stage PFE Data");
        when(annonceRepository.save(any(AnnonceStage.class))).thenAnswer(i -> {
            AnnonceStage a = i.getArgument(0);
            a.setId(1L);
            return a;
        });

        AnnonceResponse resp = service.create(req, "admin.rh");

        assertThat(resp.getTitre()).isEqualTo("Stage PFE Data");
        ArgumentCaptor<AnnonceStage> captor = ArgumentCaptor.forClass(AnnonceStage.class);
        verify(annonceRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedPar()).isEqualTo("admin.rh");
    }

    @Test
    @DisplayName("findById sur id inconnu lève ResourceNotFoundException")
    void findById_introuvable() {
        when(annonceRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete sur id inconnu lève ResourceNotFoundException")
    void delete_introuvable() {
        when(annonceRepository.existsById(9L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(ResourceNotFoundException.class);
        verify(annonceRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("toggleActif inverse le statut de l'annonce")
    void toggleActif() {
        AnnonceStage a = new AnnonceStage();
        a.setId(1L);
        a.setActif(true);
        when(annonceRepository.findById(1L)).thenReturn(Optional.of(a));

        service.toggleActif(1L);

        assertThat(a.getActif()).isFalse();
        verify(annonceRepository).save(a);
    }
}
