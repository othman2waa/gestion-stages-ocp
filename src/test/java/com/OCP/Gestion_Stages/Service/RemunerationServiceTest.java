package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.Repository.AttestationStageRepository;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.enums.TypeStage;
import com.OCP.Gestion_Stages.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RemunerationService — liste des PFE à rémunérer par tranche")
class RemunerationServiceTest {

    @Mock private AttestationStageRepository attestationRepository;
    @InjectMocks private RemunerationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "niveauxConfig", "Bac+5,Master,Ingénieur");
    }

    private AttestationStage att(TypeStage type, String niveau, LocalDate debut, LocalDate fin,
                                 StageStatus statut, String dept) {
        Stagiaire st = new Stagiaire();
        st.setNom("Test"); st.setPrenom("Stagiaire"); st.setNiveau(niveau);
        Stage stage = new Stage();
        stage.setTypeStage(type); stage.setStagiaire(st); stage.setStatut(statut);
        stage.setDateDebut(debut); stage.setDateFin(fin); stage.setSujet("Sujet PFE");
        if (dept != null) { Departement d = new Departement(); d.setNom(dept); stage.setDepartement(d); }
        AttestationStage a = new AttestationStage();
        a.setStage(stage); a.setStatut("APPROUVEE"); a.setNumeroAttestation("ATT-1-202604");
        return a;
    }

    private void given(AttestationStage... list) {
        when(attestationRepository.findByStatutOrderByDateDemandeDesc("APPROUVEE")).thenReturn(List.of(list));
    }

    @Test
    @DisplayName("un PFE Bac+5 terminé dans la fenêtre de juillet est éligible")
    void pfeBac5Eligible() {
        given(att(TypeStage.PFE, "Bac+5", LocalDate.of(2025, 12, 31), LocalDate.of(2026, 4, 30),
                StageStatus.TERMINE, "Mining"));
        Map<String, Object> res = service.genererListe(2026, "JUILLET", null);
        assertThat(res.get("total")).isEqualTo(1);
        assertThat((List<?>) res.get("stagiaires")).hasSize(1);
        assertThat((List<?>) res.get("parDepartement")).hasSize(1);
    }

    @Test
    @DisplayName("un stage non-PFE est exclu")
    void nonPfeExclu() {
        given(att(TypeStage.STAGE_ETE, "Bac+5", LocalDate.of(2025, 12, 31), LocalDate.of(2026, 4, 30),
                StageStatus.TERMINE, "Mining"));
        assertThat(service.genererListe(2026, "JUILLET", null).get("total")).isEqualTo(0);
    }

    @Test
    @DisplayName("un niveau autre que Bac+5 est exclu")
    void niveauNonBac5Exclu() {
        given(att(TypeStage.PFE, "Bac+3", LocalDate.of(2025, 12, 31), LocalDate.of(2026, 4, 30),
                StageStatus.TERMINE, "Mining"));
        assertThat(service.genererListe(2026, "JUILLET", null).get("total")).isEqualTo(0);
    }

    @Test
    @DisplayName("un stage annulé est exclu")
    void stageAnnuleExclu() {
        given(att(TypeStage.PFE, "Bac+5", LocalDate.of(2025, 12, 31), LocalDate.of(2026, 4, 30),
                StageStatus.ANNULE, "Mining"));
        assertThat(service.genererListe(2026, "JUILLET", null).get("total")).isEqualTo(0);
    }

    @Test
    @DisplayName("un stage hors de la fenêtre de la tranche est exclu (septembre)")
    void horsFenetreExclu() {
        given(att(TypeStage.PFE, "Bac+5", LocalDate.of(2025, 12, 31), LocalDate.of(2026, 4, 30),
                StageStatus.TERMINE, "Mining"));
        // Tranche septembre 2026 = stages terminés juillet-août → avril hors fenêtre
        assertThat(service.genererListe(2026, "SEPTEMBRE", null).get("total")).isEqualTo(0);
    }

    @Test
    @DisplayName("la durée du stage est calculée en mois")
    void dureeCalculee() {
        given(att(TypeStage.PFE, "Master", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1),
                StageStatus.TERMINE, "Finance"));
        Map<String, Object> res = service.genererListe(2026, "JUILLET", null);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lignes = (List<Map<String, Object>>) res.get("stagiaires");
        assertThat(lignes).hasSize(1);
        assertThat((Double) lignes.get(0).get("dureeMois")).isGreaterThan(2.0);
    }

    @Test
    @DisplayName("le mode ad-hoc (N derniers mois) fonctionne")
    void modeAdHoc() {
        LocalDate fin = LocalDate.now().minusDays(10);
        given(att(TypeStage.PFE, "Bac+5", fin.minusMonths(3), fin, StageStatus.TERMINE, "IT"));
        Map<String, Object> res = service.genererListe(null, null, 6);
        assertThat(res.get("total")).isEqualTo(1);
        assertThat(res.get("tranche")).isEqualTo("PERSONNALISEE");
    }
}
