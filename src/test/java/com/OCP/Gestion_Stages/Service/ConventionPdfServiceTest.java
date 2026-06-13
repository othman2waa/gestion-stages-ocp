package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.domain.dto.convention.ConventionResponse;
import com.OCP.Gestion_Stages.domain.enums.ConventionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConventionPdfService — génération de la convocation PDF (smoke test)")
class ConventionPdfServiceTest {

    private final ConventionPdfService service = new ConventionPdfService();

    private ConventionResponse convocationComplete() {
        ConventionResponse c = new ConventionResponse();
        c.setId(1L);
        c.setStageId(7L);
        c.setNumero("CONV-7-2026");
        c.setStatut(ConventionStatus.EN_VALIDATION);
        c.setDateEmission(LocalDate.of(2026, 1, 5));
        c.setStageSujet("Plateforme de gestion des stages");
        c.setStagiaireNom("Amal Kozal");
        c.setStagiaireEmail("amal@example.com");
        c.setStagiaireCin("AB123456");
        c.setStagiaireFiliere("Génie informatique");
        c.setStagiaireNiveau("Bac+5");
        c.setStagiaireEtablissement("UM6P");
        c.setEncadrantNom("Karim El Amrani");
        c.setEncadrantEmail("karim@ocp.ma");
        c.setDepartementNom("Transformation digitale");
        c.setStageDebut(LocalDate.of(2026, 1, 1));
        c.setStageFin(LocalDate.of(2026, 5, 1));
        c.setTypeStage("PFE");
        c.setEntiteAccueil("Direction Capital Humain — Benguérir");
        return c;
    }

    @Test
    @DisplayName("genererPdf produit un PDF non vide commençant par %PDF")
    void genererPdf() {
        byte[] pdf = service.genererPdf(convocationComplete());
        assertThat(pdf).isNotNull().isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}
