package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.domain.dto.encadrant.EncadrantResponse;
import com.OCP.Gestion_Stages.domain.model.Encadrant;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EncadrantServiceImpl — consultation des encadrants")
class EncadrantServiceImplTest {

    @Mock private EncadrantRepository encadrantRepository;
    @Mock private DepartementRepository departementRepository;
    @Mock private SuiviHebdomadaireRepository suiviRepository;
    @Mock private UserRepository userRepository;
    @Mock private StageRepository stageRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private EncadrantServiceImpl service;

    private Encadrant enc(Long id) {
        Encadrant e = new Encadrant();
        e.setId(id); e.setNom("El Amrani"); e.setPrenom("Karim");
        e.setEmail("karim@ocp.ma"); e.setFonction("Ingénieur");
        return e;
    }

    @Test
    @DisplayName("findAll mappe les encadrants")
    void findAllMappe() {
        when(encadrantRepository.findAll()).thenReturn(List.of(enc(1L), enc(2L)));
        List<EncadrantResponse> list = service.findAll();
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getNom()).isEqualTo("El Amrani");
    }

    @Test
    @DisplayName("findById renvoie l'encadrant mappé")
    void findByIdOk() {
        when(encadrantRepository.findById(1L)).thenReturn(Optional.of(enc(1L)));
        EncadrantResponse r = service.findById(1L);
        assertThat(r.getPrenom()).isEqualTo("Karim");
    }

    @Test
    @DisplayName("findById sur un id inconnu lève ResourceNotFoundException")
    void findByIdIntrouvable() {
        when(encadrantRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(9L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
