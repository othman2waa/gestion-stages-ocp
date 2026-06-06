package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.FileStorageService;
import com.OCP.Gestion_Stages.Service.interfaces.ConventionServiceExtended;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.DocumentStagiaireResponse;
import com.OCP.Gestion_Stages.domain.model.DocumentStagiaire;
import com.OCP.Gestion_Stages.domain.model.Stagiaire;
import com.OCP.Gestion_Stages.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentStagiaireServiceImpl — stockage disque + projection")
class DocumentStagiaireServiceImplTest {

    @Mock private DocumentStagiaireRepository documentRepository;
    @Mock private StagiaireRepository stagiaireRepository;
    @Mock private UserRepository userRepository;
    @Mock private StageRepository stageRepository;
    @Mock private ConventionRepository conventionRepository;
    @Mock private ConventionServiceExtended conventionServiceExtended;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks private DocumentStagiaireServiceImpl service;

    private Stagiaire stagiaire;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).username("sara").build();
        stagiaire = new Stagiaire();
        stagiaire.setId(7L);
        when(userRepository.findByUsername("sara")).thenReturn(Optional.of(user));
        when(stagiaireRepository.findByUserId(1L)).thenReturn(Optional.of(stagiaire));
    }

    @Test
    @DisplayName("upload écrit le fichier sur disque et ne stocke PAS le contenu en base")
    void upload_ecrit_sur_disque() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "data".getBytes());
        when(documentRepository.findByStagiaireIdAndTypeDocument(7L, "CV")).thenReturn(Optional.empty());
        when(fileStorageService.store(any(), eq("cv.pdf"))).thenReturn("uuid_cv.pdf");
        when(documentRepository.save(any(DocumentStagiaire.class))).thenAnswer(i -> i.getArgument(0));
        // dossier incomplet -> pas d'auto-génération de convention
        when(documentRepository.existsByStagiaireIdAndTypeDocument(anyLong(), anyString())).thenReturn(false);

        service.upload("sara", "CV", file);

        ArgumentCaptor<DocumentStagiaire> captor = ArgumentCaptor.forClass(DocumentStagiaire.class);
        verify(documentRepository).save(captor.capture());
        DocumentStagiaire saved = captor.getValue();
        assertThat(saved.getCheminFichier()).isEqualTo("uuid_cv.pdf");
        assertThat(saved.getContenu()).isNull();
        verify(fileStorageService).store(any(), eq("cv.pdf"));
    }

    @Test
    @DisplayName("delete supprime aussi le fichier sur disque")
    void delete_supprime_fichier_disque() {
        DocumentStagiaire doc = DocumentStagiaire.builder()
                .id(5L).stagiaire(stagiaire).typeDocument("CV").cheminFichier("uuid_cv.pdf").build();
        when(documentRepository.findById(5L)).thenReturn(Optional.of(doc));

        service.delete("sara", 5L);

        verify(fileStorageService).delete("uuid_cv.pdf");
        verify(documentRepository).delete(doc);
    }

    @Test
    @DisplayName("getMesDocuments utilise la projection métadonnées (pas le byte[])")
    void getMesDocuments_utilise_projection() {
        Object[] row = {10L, "CV", "cv.pdf", "application/pdf", 123L, LocalDateTime.now()};
        when(documentRepository.findMetaByStagiaireId(7L)).thenReturn(List.<Object[]>of(row));

        List<DocumentStagiaireResponse> docs = service.getMesDocuments("sara");

        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).getTypeDocument()).isEqualTo("CV");
        assertThat(docs.get(0).getNomFichier()).isEqualTo("cv.pdf");
        verify(documentRepository, never()).findByStagiaireId(anyLong());
    }
}
