package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.DepartementRepository;
import com.OCP.Gestion_Stages.Repository.EncadrantRepository;
import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.Repository.StagiaireRepository;
import com.OCP.Gestion_Stages.domain.dto.departement.DepartementRequest;
import com.OCP.Gestion_Stages.domain.dto.departement.DepartementResponse;
import com.OCP.Gestion_Stages.domain.model.Departement;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DepartementServiceImpl — CRUD départements")
class DepartementServiceImplTest {

    @Mock private DepartementRepository departementRepository;
    @Mock private EncadrantRepository encadrantRepository;
    @Mock private StageRepository stageRepository;
    @Mock private StagiaireRepository stagiaireRepository;

    @InjectMocks private DepartementServiceImpl service;

    private Departement dept;

    @BeforeEach
    void setUp() {
        dept = new Departement();
        dept.setId(1L);
        dept.setCode("IT");
        dept.setNom("Informatique");
        dept.setActif(true);
        // compteurs utilisés par toResponse
        when(encadrantRepository.countByDepartementId(anyLong())).thenReturn(2L);
        when(stageRepository.countByDepartementId(anyLong())).thenReturn(5L);
        when(stageRepository.countByDepartementIdAndStatut(anyLong(), any())).thenReturn(1L);
        when(stagiaireRepository.countByDepartementId(anyLong())).thenReturn(3L);
    }

    @Test
    @DisplayName("findAll mappe les départements (avec compteurs)")
    void findAll_mappe() {
        when(departementRepository.findAll()).thenReturn(List.of(dept));
        List<DepartementResponse> list = service.findAll();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getCode()).isEqualTo("IT");
        assertThat(list.get(0).getNombreEncadrants()).isEqualTo(2);
    }

    @Test
    @DisplayName("findById sur id inconnu lève ResourceNotFoundException")
    void findById_introuvable() {
        when(departementRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create avec code déjà utilisé est rejeté")
    void create_code_existant() {
        DepartementRequest req = new DepartementRequest();
        req.setCode("IT");
        when(departementRepository.existsByCode("IT")).thenReturn(true);
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("déjà utilisé");
        verify(departementRepository, never()).save(any());
    }

    @Test
    @DisplayName("create valide sauvegarde et renvoie le département")
    void create_ok() {
        DepartementRequest req = new DepartementRequest();
        req.setCode("FIN");
        req.setNom("Finance");
        when(departementRepository.existsByCode("FIN")).thenReturn(false);
        when(departementRepository.save(any(Departement.class))).thenAnswer(i -> {
            Departement d = i.getArgument(0);
            d.setId(2L);
            return d;
        });
        DepartementResponse resp = service.create(req);
        assertThat(resp.getCode()).isEqualTo("FIN");
        verify(departementRepository).save(any(Departement.class));
    }

    @Test
    @DisplayName("delete sur id inconnu lève ResourceNotFoundException")
    void delete_introuvable() {
        when(departementRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(departementRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("toggleActif inverse le statut actif")
    void toggleActif() {
        dept.setActif(true);
        when(departementRepository.findById(1L)).thenReturn(Optional.of(dept));
        service.toggleActif(1L);
        assertThat(dept.getActif()).isFalse();
        verify(departementRepository).save(dept);
    }
}
