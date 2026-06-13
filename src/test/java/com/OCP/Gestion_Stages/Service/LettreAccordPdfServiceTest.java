package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.domain.enums.TypeStage;
import com.OCP.Gestion_Stages.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LettreAccordPdfService — génération de la lettre d'accord PFE (smoke test)")
class LettreAccordPdfServiceTest {

    private final LettreAccordPdfService service = new LettreAccordPdfService();

    @Test
    @DisplayName("genererLettreAccord produit un PDF non vide")
    void genererLettreAccord() {
        Etablissement etab = new Etablissement();
        etab.setNom("Université Mohammed VI Polytechnique");

        Stagiaire st = new Stagiaire();
        st.setNom("Kozal"); st.setPrenom("Amal");
        st.setFiliere("Génie informatique"); st.setNiveau("Bac+5");
        st.setEtablissement(etab);

        Encadrant enc = new Encadrant();
        enc.setNom("El Amrani"); enc.setPrenom("Karim");

        Departement dept = new Departement();
        dept.setNom("Transformation digitale"); dept.setCode("DT");

        Stage stage = new Stage();
        stage.setId(7L); stage.setSujet("Plateforme de gestion des stages");
        stage.setStagiaire(st); stage.setEncadrant(enc); stage.setDepartement(dept);
        stage.setTypeStage(TypeStage.PFE);
        stage.setDateDebut(LocalDate.of(2026, 1, 1));
        stage.setDateFin(LocalDate.of(2026, 5, 1));

        byte[] pdf = service.genererLettreAccord(stage);
        assertThat(pdf).isNotNull().isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}
